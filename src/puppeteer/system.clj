(ns puppeteer.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [puppeteer.infra.client.container-builder :refer [container-builder-client-component]]
            [puppeteer.infra.client.pubsub :refer [pubsub-subscription-component]]
            [puppeteer.infra.client.k8s :refer [k8s-client-component]]
            [puppeteer.infra.client.slack :refer [slack-component]]
            [puppeteer.infra.client.slack-rtm :refer [slack-rtm-component]]
            [puppeteer.infra.client.github :refer [github-component]]
            [puppeteer.infra.repository.build :refer [build-repository-component]]
            [puppeteer.infra.repository.deploy :refer [deploy-repository-component]]
            [puppeteer.infra.repository.message :refer [message-repository-component]]
            [puppeteer.domain.usecase.build :refer [build-usecase-component]]
            [puppeteer.domain.usecase.message :refer [message-usecase-component]]
            [puppeteer.app.webapp.handler :refer [webapp-handler-component]]
            [puppeteer.app.webapp.endpoint :refer [webapp-endpoint-component]]
            [puppeteer.app.slackbot.alice :refer [alice-component]])
  (:gen-class))

(defn puppeteer-system
  [{:keys [puppeteer-k8s-endpoint
           puppeteer-webapp-port
           puppeteer-slack-token
           puppeteer-github-oauth-token] :as conf}]
  (component/system-map
    :container-builder-client (container-builder-client-component)
    :pubsub-subscription (pubsub-subscription-component)
    :k8s-client (k8s-client-component puppeteer-k8s-endpoint)
    :slack-client (slack-component puppeteer-slack-token)
    :slack-rtm-client (slack-rtm-component puppeteer-slack-token)
    :github-client (github-component puppeteer-github-oauth-token)
    :build-repository (component/using
                        (build-repository-component)
                        [:container-builder-client
                         :pubsub-subscription])
    :deploy-repository (component/using
                         (deploy-repository-component)
                         [:k8s-client])
    :message-repository (component/using
                          (message-repository-component)
                          [:slack-client
                           :slack-rtm-client])
    :build-usecase (component/using
                     (build-usecase-component)
                     [:build-repository])
    :message-usecase (component/using
                       (message-usecase-component)
                       [:message-repository])
    :webapp-handler (webapp-handler-component)
    :webapp-endpoint (component/using
                       (webapp-endpoint-component puppeteer-webapp-port)
                       [:webapp-handler])
    :alice (component/using
             (alice-component)
             [:message-usecase
              :build-usecase])))

(defn load-config []
  {:puppeteer-webapp-port (-> (or (env :puppeteer-webapp-port) "8080") Integer/parseInt)
   :puppeteer-k8s-endpoint (or (env :puppeteer-k8s-endpoint) "http://localhost:8001")
   :puppeteer-slack-token (env :puppeteer-slack-token)
   :puppeteer-github-oauth-token (env :puppeteer-github-oauth-token)})

(defn -main []
  (component/start
    (puppeteer-system (load-config))))
