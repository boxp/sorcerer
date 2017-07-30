(ns puppeteer.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [puppeteer.infra.client.container-builder :refer [container-builder-client-component]]
            [puppeteer.infra.client.pubsub :refer [pubsub-subscription-component]]
            [puppeteer.infra.client.k8s :refer [k8s-client-component]]
            [puppeteer.infra.client.slack :refer [slack-component]]
            [puppeteer.infra.client.slack-rtm :refer [slack-rtm-component]]
            [puppeteer.infra.client.github :refer [github-component]]
            [puppeteer.infra.client.dynamodb :refer [dynamodb-component]]
            [puppeteer.infra.repository.build :refer [build-repository-component]]
            [puppeteer.infra.repository.deploy :refer [deploy-repository-component]]
            [puppeteer.infra.repository.message :refer [message-repository-component]]
            [puppeteer.infra.repository.conf :refer [configuration-repository-component]]
            [puppeteer.infra.repository.job :refer [job-repository-component]]
            [puppeteer.domain.usecase.build :refer [build-usecase-component]]
            [puppeteer.domain.usecase.message :refer [message-usecase-component]]
            [puppeteer.domain.usecase.conf :refer [configuration-usecase-component]]
            [puppeteer.domain.usecase.job :refer [job-usecase-component]]
            [puppeteer.domain.usecase.deploy :refer [deploy-usecase-component]]
            [puppeteer.app.slackbot.alice :refer [alice-component]])
  (:gen-class))

(defn puppeteer-system
  [{:keys [puppeteer-k8s-endpoint
           puppeteer-webapp-port
           puppeteer-slack-token
           puppeteer-github-oauth-token
           puppeteer-aws-access-key
           puppeteer-aws-secret-key
           puppeteer-dynamodb-endpoint
           puppeteer-pubsub-subscription-name
           puppeteer-k8s-ingress-name
           puppeteer-k8s-domain] :as conf}]
  (component/system-map
    :container-builder-client (container-builder-client-component)
    :pubsub-subscription (pubsub-subscription-component)
    :k8s-client (k8s-client-component puppeteer-k8s-endpoint)
    :slack-client (slack-component puppeteer-slack-token)
    :slack-rtm-client (slack-rtm-component puppeteer-slack-token)
    :github-client (github-component puppeteer-github-oauth-token)
    :dynamodb-client (dynamodb-component puppeteer-aws-access-key puppeteer-aws-secret-key puppeteer-dynamodb-endpoint)
    :build-repository (component/using
                        (build-repository-component puppeteer-pubsub-subscription-name)
                        [:container-builder-client
                         :pubsub-subscription])
    :deploy-repository (component/using
                         (deploy-repository-component puppeteer-k8s-domain puppeteer-k8s-ingress-name)
                         [:k8s-client
                          :github-client])
    :message-repository (component/using
                          (message-repository-component)
                          [:slack-client
                           :slack-rtm-client])
    :conf-repository (component/using
                       (configuration-repository-component)
                       [:github-client])
    :job-repository (component/using
                      (job-repository-component)
                      [:dynamodb-client])
    :build-usecase (component/using
                     (build-usecase-component)
                     [:build-repository
                      :conf-repository])
    :message-usecase (component/using
                       (message-usecase-component)
                       [:message-repository])
    :conf-usecase (component/using
                    (configuration-usecase-component)
                    [:conf-repository])
    :job-usecase (component/using
                   (job-usecase-component)
                   [:job-repository])
    :deploy-usecase (component/using
                      (deploy-usecase-component)
                      [:deploy-repository])
    :alice (component/using
             (alice-component)
             [:message-usecase
              :build-usecase
              :conf-usecase
              :job-usecase
              :deploy-usecase])))

(defn load-config []
  {:puppeteer-k8s-endpoint (env :puppeteer-k8s-endpoint)
   :puppeteer-slack-token (env :puppeteer-slack-token)
   :puppeteer-github-oauth-token (env :puppeteer-github-oauth-token)
   :puppeteer-aws-access-key (env :puppeteer-aws-access-key)
   :puppeteer-aws-secret-key (env :puppeteer-aws-secret-key)
   :puppeteer-dynamodb-endpoint (env :puppeteer-dynamodb-endpoint)
   :puppeteer-pubsub-subscription-name (env :puppeteer-pubsub-subscription-name)
   :puppeteer-k8s-ingress-name (env :puppeteer-k8s-ingress-name)
   :puppeteer-k8s-domain (env :puppeteer-k8s-domain)})

(defn -main []
  (component/start
    (puppeteer-system (load-config))))
