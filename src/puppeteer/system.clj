(ns puppeteer.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [puppeteer.infra.client.container-builder :refer [container-builder-client-component]]
            [puppeteer.infra.client.pubsub :refer [pubsub-subscription-component]]
            [puppeteer.infra.client.k8s :refer [k8s-client-component]]
            [puppeteer.infra.repository.build :refer [build-repository-component]]
            [puppeteer.infra.repository.deploy :refer [deploy-repository-component]]
            [puppeteer.domain.usecase.build :refer [build-usecase-component]]
            [puppeteer.app.webapp.handler :refer [webapp-handler-component]]
            [puppeteer.app.webapp.endpoint :refer [webapp-endpoint-component]])
  (:gen-class))

(defn puppeteer-system
  [{:keys [puppeteer-k8s-endpoint
           puppeteer-webapp-port] :as conf}]
  (component/system-map
    :container-builder-client (container-builder-client-component)
    :pubsub-subscription (pubsub-subscription-component)
    :k8s-client (k8s-client-component puppeteer-k8s-endpoint)
    :build-repository (component/using
                        (build-repository-component)
                        [:container-builder-client
                         :pubsub-subscription])
    :deploy-repository (component/using
                         (deploy-repository-component)
                         [:k8s-client])
    :build-usecase (component/using
                     (build-usecase-component)
                     [:build-repository])
    :webapp-handler (webapp-handler-component)
    :webapp-endpoint (component/using
                       (webapp-endpoint-component puppeteer-webapp-port)
                       [:webapp-handler])))

(defn load-config []
  {:puppeteer-webapp-port (-> (or (env :puppeteer-webapp-port) "8080") Integer/parseInt)
   :puppeteer-k8s-endpoint (-> (or (env :puppeteer-k8s-endpoint) "http://localhost:8001"))})

(defn -main []
  (component/start
    (puppeteer-system (load-config))))
