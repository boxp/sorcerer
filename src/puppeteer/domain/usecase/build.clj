(ns puppeteer.domain.usecase.build
  (:require [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.build :as entity]
            [puppeteer.infra.repository.build :as r]))

(defn example-build
  [{:keys [build-repository] :as comp}]
  (let [build {:source {:repo-source {:project-id "boxp-tk"
                                      :repo-name "eure-atnd"
                                      :branch-name "master"}}
               :steps [{:name "gcr.io/cloud-builders/docker"
                        :args ["build", "-t", "asia.gcr.io/$PROJECT_ID/eure-atnd:$COMMIT_SHA", "."]}]
               :images ["asia.gcr.io/$PROJECT_ID/eure-atnd:$COMMIT_SHA"]}]
    (r/create-build build-repository build)))

(defn subscribe-build-message
  [{:keys [build-repository] :as comp}]
  (r/get-build-message build-repository))

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
