(ns puppeteer.infra.repository.build
  (:import (com.google.api.services.cloudbuild.v1.model Build Source RepoSource BuildStep))
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go put! <! close! chan]]
            [cheshire.core :refer [parse-string]]
            [puppeteer.domain.entity.build :as entity]
            [puppeteer.infra.client.container-builder :as gccb-cli]
            [puppeteer.infra.client.pubsub :as pubsub-cli]))

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
                      (.setName (-> % :name)))
                   (-> build :steps))
        images (-> build :images)]
    (doto (Build.)
      (.setSource source)
      (.setSteps steps)
      (.setImages images))))

(defn- BuildMessage->build-message
  [m]
  (some-> m
          .getData
          .toStringUtf8
          parse-string
          entity/map->BuildMessage))

(defn create-build
  [{:keys [container-builder-client] :as comp} build]
  (->> build
       build->Build
       (gccb-cli/create-build container-builder-client)))

(def topic-key :cloud-builds)
(def subscription-key :puppeteer-cloud-builds)

(defn get-build-message
  [{:keys [container-builder-client] :as comp}]
  (:channel comp))

(defrecord BuildRepositoryComponent [container-builder-client pubsub-subscription]
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
    (close! (:channel this))
    (-> this
        (dissoc :channel))))

(defn build-repository-component
  []
  (map->BuildRepositoryComponent {}))
