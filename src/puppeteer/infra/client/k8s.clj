(ns puppeteer.infra.client.k8s
  (:import (io.fabric8.kubernetes.client DefaultKubernetesClient))
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(s/def ::endpoint string?)
(s/def ::k8s-client-component
  (s/keys :req-un [::endpoint]))

(defrecord K8sClientComponent [project-id client endpoint]
  component/Lifecycle
  (start [this]
    (println ";; Starting K8sClientComponent")
    (-> this
        (assoc :client (if endpoint
                         (DefaultKubernetesClient. endpoint)
                         (DefaultKubernetesClient.)))))
  (stop [this]
    (println ";; Stopping K8sClientComponent")
    (-> this :client .close)
    (-> this
        (dissoc :client))))

(s/fdef k8s-client-component
  :args (s/cat :endpoint ::endpoint)
  :ret ::k8s-client-component)
(defn k8s-client-component
  [endpoint]
  (map->K8sClientComponent {:endpoint endpoint}))
