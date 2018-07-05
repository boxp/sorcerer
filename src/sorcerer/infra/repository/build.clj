(ns sorcerer.infra.repository.build
  (:import (com.google.api.services.cloudbuild.v1.model Build Source RepoSource BuildStep Secret Volume)
           (com.google.pubsub.v1 PubsubMessage))
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go put! <! close! chan]]
            [com.stuartsierra.component :as component]
            [cheshire.core :refer [parse-string]]
            [sorcerer.domain.entity.build :as entity]
            [sorcerer.infra.client.container-builder :as gccb-cli]
            [sorcerer.infra.client.pubsub :as pubsub-cli]))

(s/def ::subscription-name string?)
(s/def ::subscription-key keyword?)
(s/def ::message-channel #(instance? (-> (chan) class) %))
(s/def :build-repository-component/container-builder-client ::gccb-cli/container-builder-client-component)
(s/def :build-repository-component/pubsub-subscription ::pubsub-cli/pubsub-subscription-component)
(s/def ::build-repository-component
  (s/keys :req-un [::subscription-key]
          :opt-un [:build-repository-component/container-builder-client
                   :build-repository-component/pubsub-subscription]))

(def default-build-timeout
  "600.0s")

(s/fdef volume->Volume
  :args (s/cat :volume ::entity/volume)
  :ret #(instance? Volume %))
(defn- volume->Volume
  [volume]
  (doto (Volume.)
    (.setName (:name volume))
    (.setPath (:path volume))))


(s/fdef build->Build
  :args (s/cat :build ::entity/build)
  :ret #(instance? Build %))
(defn- build->Build
  [build]
  (let [repo-source (doto (RepoSource.)
                      (.setProjectId (-> build :source :repo-source :project-id))
                      (.setRepoName (-> build :source :repo-source :repo-name))
                      (.setBranchName (-> build :source :repo-source :branch-name))
                      (.setTagName (-> build :source :repo-source :tag-name))
                      (.setCommitSha (-> build :source :repo-source :commit-sha)))
        source (doto (Source.)
                 (.setRepoSource repo-source))
        steps (map #(doto (BuildStep.)
                      (.setArgs (-> % :args))
                      (.setName (-> % :name))
                      (.setEntrypoint (-> % :entrypoint))
                      (.setEnv (or (:env %) []))
                      (.setSecretEnv (or (:secretEnv %) []))
                      (.setVolumes (or (map volume->Volume (:volumes %)) [])))
                   (-> build :steps))
        images (-> build :images)
        timeout (or (:timeout build) default-build-timeout)
        secrets (map #(doto (Secret.)
                        (.setKmsKeyName (:kmsKeyName %))
                        (.setSecretEnv (java.util.HashMap. (:secretEnv %))))
                     (or (:secrets build) []))]
    (doto (Build.)
      (.setSource source)
      (.setSteps steps)
      (.setImages images)
      (.setTimeout timeout)
      (.setSecrets secrets))))

(s/fdef BuildMessage->build-message
  :args (s/cat :m #(instance? PubsubMessage %))
  :ret ::entity/build-message)
(defn- BuildMessage->build-message
  [m]
  (some-> m
          .getData
          .toStringUtf8
          (parse-string true)))

(s/fdef Operation->BuildId
  :args (s/cat :operation true?)
  :ret string?)
(defn- Operation->BuildId
  [operation]
  (some-> operation
          .getMetadata
          (.get "build")
          (.get "id")))

(s/fdef create-build
  :args (s/cat :comp ::build-repository-component
               :build ::entity/build)
  :ret string?)
(defn create-build
  [{:keys [container-builder-client] :as comp} build]
  (->> build
       build->Build
       (gccb-cli/create-build container-builder-client)
       Operation->BuildId))

(def topic-key :cloud-builds)

(s/fdef get-build-message
  :args (s/cat :comp ::build-repository-component)
  :ret ::message-channel)
(defn get-build-message
  [{:keys [container-builder-client] :as comp}]
  (:channel comp))

(defrecord BuildRepositoryComponent [container-builder-client pubsub-subscription subscription-key]
  component/Lifecycle
  (start [this]
    (let [c (chan)]
      (println ";; Starting BuildRepositoryComponent")
      (try
        (pubsub-cli/create-subscription (:pubsub-subscription this) topic-key subscription-key)
        (catch Exception e (println "Warning: Already" subscription-key "has exists")))
      (-> this
          (update :pubsub-subscription
                  #(pubsub-cli/add-subscriber % topic-key subscription-key
                                              (fn [m]
                                                (put! c (BuildMessage->build-message m)))))
          (assoc :channel c))))
  (stop [this]
    (println ";; Stopping BuildRepositoryComponent")
    (doall (map #(.stopAsync %) (-> this :pubsub-subscription :subscribers)))
    (close! (:channel this))
    (-> this
        (dissoc :channel))))

(s/fdef build-repository-component
  :args (s/cat :subscription-name ::subscription-name)
  :ret ::build-repository-component)
(defn build-repository-component
  [subscription-name]
  (map->BuildRepositoryComponent {:subscription-key (keyword subscription-name)}))
