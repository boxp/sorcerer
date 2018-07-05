(ns sorcerer.infra.repository.job
  (:require [com.stuartsierra.component :as component]
            [taoensso.faraday :as far]
            [sorcerer.infra.client.dynamodb :as dynamodbrepo]
            [sorcerer.domain.entity.job :refer [map->Job]]))

(defn get-job
  [{:keys [dynamodb-client] :as comp}
   id]
  (some-> (far/get-item (:opts dynamodb-client) :sorcerer-job {:id id})
          map->Job))

(defn set-job
  [{:keys [dynamodb-client] :as comp}
   {:keys [conf message build user-name repo-name branch-name subdomain] :as job}]
  (far/update-item (:opts dynamodb-client) :sorcerer-job
                   {:id (-> job :build :id)}
                   {:conf [:put (far/freeze conf)]
                    :message [:put (far/freeze message)]
                    :build [:put (far/freeze build)]
                    :user-name [:put user-name]
                    :repo-name [:put repo-name]
                    :branch-name [:put branch-name]
                    :subdomain [:put subdomain]}))


(defrecord JobRepositoryComponent [dynamodb-client]
  component/Lifecycle
  (start [this]
    (println ";; Starting JobRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping JobRepositoryComponent")
    this))

(defn job-repository-component
  []
  (map->JobRepositoryComponent {}))
