(ns puppeteer.infra.repository.deploy
  (:require [com.stuartsierra.component :as component]))

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
