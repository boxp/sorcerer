(ns sorcerer.infra.repository.search
  (:require [clojure.spec.alpha :as s]
            [clj-yaml.core :as yaml]
            [com.stuartsierra.component :as component]
            [sorcerer.infra.client.k8s :as k8s]
            [sorcerer.domain.entity.search :as search])
  (:import (com.fasterxml.jackson.dataformat.yaml YAMLFactory)
           (com.fasterxml.jackson.databind ObjectMapper)))

(s/def :search-repository-component/k8s-client ::k8s/k8s-client-component)
(s/def ::search-repository-component
  (s/keys :opt-un [:search-repository-component/k8s-client]))

(s/fdef get-all-deployments
  :args (s/cat :c :search-repository-component)
  :ret (s/coll-of ::search/deployment))
(defn get-all-deployments
  [{:keys [k8s-client] :as c}]
  (as-> (some-> k8s-client
                :client
                .extensions
                .deployments
                .list
                .getItems) $
       (.writeValueAsString (ObjectMapper. (YAMLFactory.)) $)
       (yaml/parse-string $ :keywords true)))

(s/fdef search-deployments
  :args (s/cat :c :search-repository-component
               :query string?)
  :ret (s/coll-of ::search/deployment))
(defn search-deployments
  [{:keys [k8s-client] :as c} query]
  (->> (get-all-deployments c)
       (filter #(some->> %
                         :metadata
                         :name
                         (re-find (-> query clojure.string/re-quote-replacement re-pattern))))))

(defrecord SearchRepositoryComponent [k8s-client]
  component/Lifecycle
  (start [this]
    (println ";; Starting SearchRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping SearchRepositoryComponent")
    this))
