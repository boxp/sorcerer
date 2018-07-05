(ns sorcerer.domain.usecase.conf
  (:require [com.stuartsierra.component :as component]
            [sorcerer.infra.repository.conf :as confrepo]
            [sorcerer.domain.entity.conf :refer [map->Configuration]]))

(defn load-conf
  [{:keys [conf-repository] :as comp}
   {:keys [user-name repo-name branch-name] :as opts}]
  (confrepo/load-conf conf-repository opts))

(defrecord ConfigurationUsecaseComponent [conf-repository]
  component/Lifecycle
  (start [this]
    (println ";; Starting ConfigurationUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping ConfigurationUsecaseComponent")
    this))

(defn configuration-usecase-component
  []
  (map->ConfigurationUsecaseComponent {}))
