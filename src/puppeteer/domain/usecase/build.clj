(ns puppeteer.domain.usecase.build
  (:import (com.google.cloud ServiceOptions))
  (:require [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.build :as entity]
            [puppeteer.infra.repository.build :as buildrepo]
            [puppeteer.infra.repository.conf :as confrepo]))

(defn example-build
  [{:keys [build-repository] :as comp}]
  (let [build {:source {:repo-source {:project-id "boxp-tk"
                                      :repo-name "eure-atnd"
                                      :branch-name "master"}}
               :steps [{:name "gcr.io/cloud-builders/docker"
                        :args ["build", "-t", "asia.gcr.io/$PROJECT_ID/eure-atnd:$COMMIT_SHA", "."]}]
               :images ["asia.gcr.io/$PROJECT_ID/eure-atnd:$COMMIT_SHA"]}]
    (buildrepo/create-build build-repository build)))

(defn build
  [{:keys [build-repository conf-repository] :as comp}
   {:keys [conf user-name repo-name branch-name]}]
  (let [build {:source {:repo-source {:project-id (ServiceOptions/getDefaultProjectId)
                                      :repo-name repo-name
                                      :branch-name branch-name}}
               :steps (:steps conf)
               :images (some->> conf :images vals)}]
    (buildrepo/create-build build-repository build)))

(defn subscribe-build-message
  [{:keys [build-repository] :as comp}]
  (buildrepo/get-build-message build-repository))

(defrecord BuildUsecaseComponent [build-repository conf-repository]
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
