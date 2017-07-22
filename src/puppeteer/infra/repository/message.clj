(ns puppeteer.infra.repository.message
  (:require [com.stuartsierra.component :as component]))

(defrecord MessageRepositoryComponent [slack-rtm-client slack-client]
  component/Lifecycle
  (start [{:keys [slack-client slack-rtm-client] :as this}]
    (println ";; Starting MessageRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping MessageRepositoryComponent")
    this))

(defn message-repository-component
  []
  (map->MessageRepositoryComponent {}))
