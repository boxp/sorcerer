(ns sorcerer.domain.usecase.build
  (:import (com.google.cloud ServiceOptions))
  (:require [com.stuartsierra.component :as component]
            [sorcerer.domain.entity.build :as entity]
            [sorcerer.infra.repository.build :as buildrepo]
            [sorcerer.infra.repository.conf :as confrepo]))

(defn build
  [{:keys [build-repository] :as comp}
   {:keys [conf user-name repo-name branch-name]}]
  (let [build {:source {:repo-source {:project-id (ServiceOptions/getDefaultProjectId)
                                      :repo-name repo-name
                                      :branch-name branch-name}}
               :steps (:steps conf)
               :images (some->> conf :images vals)
               :timeout (:timeout conf)
               :secrets (:secrets conf)}]
    (buildrepo/create-build build-repository build)))

(defn subscribe-build-message
  [{:keys [build-repository] :as comp}]
  (buildrepo/get-build-message build-repository))

(defrecord BuildUsecaseComponent [build-repository]
  component/Lifecycle
  (start [this]
    (println ";; Starting BuildUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping BuildUsecaseComponent")
    this))

(defn build-usecase-component
  []
  (map->BuildUsecaseComponent {}))
