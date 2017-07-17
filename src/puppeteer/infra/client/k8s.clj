(ns puppeteer.infra.client.k8s
  (:import (io.fabric8.kubernetes.client DefaultKubernetesClient))
  (:require [com.stuartsierra.component :as component]))

(defrecord K8sClientComponent [project-id client endpoint]
  component/Lifecycle
  (start [this]
    (println ";; Starting K8sClientComponent")
    (-> this
        (assoc :client (DefaultKubernetesClient. (:endpoint this)))))
  (stop [this]
    (println ";; Stopping K8sClientComponent")
    (-> this
        (dissoc :client))))

(defn k8s-client-component
  [endpoint]
  (map->K8sClientComponent {:endpoint endpoint}))
