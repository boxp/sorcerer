(ns puppeteer.infra.repository.deploy
  (:import (com.google.api.services.cloudbuild.v1.model Build Source RepoSource BuildStep))
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go put! <! close! chan]]
            [cheshire.core :refer [parse-string]]))

(defn get-ingress
  [{:keys [k8s-client] :as comp} ingress-name]
  (-> k8s-client
      :client
      .extensions
      .ingresses
      (.withName ingress-name)
      .list
      .getItems
      first))

(defrecord DeployRepositoryComponent [k8s-client]
  component/Lifecycle
  (start [this]
    (println ";; Starting BuildRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping BuildRepositoryComponent")
    this))

(defn deploy-repository-component
  []
  (map->DeployRepositoryComponent {}))
