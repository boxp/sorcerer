(ns sorcerer.domain.usecase.job
  (:require [com.stuartsierra.component :as component]
            [sorcerer.infra.repository.job :as jobrepo]))

(defn get-job
  [{:keys [job-repository]}
   id]
  (jobrepo/get-job job-repository id))

(defn set-job
  [{:keys [job-repository]}
   job]
  (jobrepo/set-job job-repository job))

(defrecord JobUsecaseComponent [job-repository]
  component/Lifecycle
  (start [this]
    (println ";; Starting JobUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping JobUsecaseComponent")
    this))

(defn job-usecase-component
  []
  (map->JobUsecaseComponent {}))
