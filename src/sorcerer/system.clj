(ns sorcerer.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [sorcerer.infra.client.container-builder :refer [container-builder-client-component]]
            [sorcerer.infra.client.pubsub :refer [pubsub-subscription-component]]
            [sorcerer.infra.client.k8s :refer [k8s-client-component]]
            [sorcerer.infra.client.slack :refer [slack-component]]
            [sorcerer.infra.client.slack-rtm :refer [slack-rtm-component]]
            [sorcerer.infra.client.github :refer [github-component]]
            [sorcerer.infra.client.dynamodb :refer [dynamodb-component]]
            [sorcerer.infra.client.cloud-dns :refer [cloud-dns-component]]
            [sorcerer.infra.repository.build :refer [build-repository-component]]
            [sorcerer.infra.repository.deploy :refer [deploy-repository-component]]
            [sorcerer.infra.repository.message :refer [message-repository-component]]
            [sorcerer.infra.repository.conf :refer [configuration-repository-component]]
            [sorcerer.infra.repository.job :refer [job-repository-component]]
            [sorcerer.infra.repository.search :refer [map->SearchRepositoryComponent]]
            [sorcerer.domain.usecase.build :refer [build-usecase-component]]
            [sorcerer.domain.usecase.message :refer [message-usecase-component]]
            [sorcerer.domain.usecase.conf :refer [configuration-usecase-component]]
            [sorcerer.domain.usecase.job :refer [job-usecase-component]]
            [sorcerer.domain.usecase.deploy :refer [deploy-usecase-component]]
            [sorcerer.domain.usecase.search :refer [map->SearchUsecaseComponent]]
            [sorcerer.app.slackbot.alice :refer [alice-component]])
  (:gen-class))

(defn sorcerer-system
  [{:keys [sorcerer-k8s-endpoint
           sorcerer-webapp-port
           sorcerer-slack-token
           sorcerer-github-oauth-token
           sorcerer-aws-access-key
           sorcerer-aws-secret-key
           sorcerer-dynamodb-endpoint
           sorcerer-pubsub-subscription-name
           sorcerer-k8s-ingress-name
           sorcerer-k8s-domain
           sorcerer-dns-zone] :as conf}]
  (component/system-map
    :container-builder-client (container-builder-client-component)
    :pubsub-subscription (pubsub-subscription-component)
    :k8s-client (k8s-client-component sorcerer-k8s-endpoint)
    :slack-client (slack-component sorcerer-slack-token)
    :slack-rtm-client (slack-rtm-component sorcerer-slack-token)
    :github-client (github-component sorcerer-github-oauth-token)
    :dynamodb-client (dynamodb-component sorcerer-aws-access-key sorcerer-aws-secret-key sorcerer-dynamodb-endpoint)
    :cloud-dns-client (cloud-dns-component sorcerer-dns-zone)
    :build-repository (component/using
                        (build-repository-component sorcerer-pubsub-subscription-name)
                        [:container-builder-client
                         :pubsub-subscription])
    :deploy-repository (component/using
                         (deploy-repository-component sorcerer-k8s-domain sorcerer-k8s-ingress-name)
                         [:k8s-client
                          :github-client
                          :cloud-dns-client])
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
    :search-repository (component/using
                         (map->SearchRepositoryComponent {})
                         [:k8s-client])
    :build-usecase (component/using
                     (build-usecase-component)
                     [:build-repository])
    :message-usecase (component/using
                       (message-usecase-component sorcerer-k8s-domain)
                       [:message-repository])
    :conf-usecase (component/using
                    (configuration-usecase-component)
                    [:conf-repository])
    :job-usecase (component/using
                   (job-usecase-component)
                   [:job-repository])
    :deploy-usecase (component/using
                      (deploy-usecase-component sorcerer-k8s-domain)
                      [:deploy-repository])
    :search-usecase (component/using
                      (map->SearchUsecaseComponent {})
                      [:search-repository])
    :alice (component/using
             (alice-component)
             [:message-usecase
              :build-usecase
              :conf-usecase
              :job-usecase
              :deploy-usecase
              :search-usecase])))

(defn load-config []
  {:sorcerer-k8s-endpoint (env :sorcerer-k8s-endpoint)
   :sorcerer-slack-token (env :sorcerer-slack-token)
   :sorcerer-github-oauth-token (env :sorcerer-github-oauth-token)
   :sorcerer-aws-access-key (env :sorcerer-aws-access-key)
   :sorcerer-aws-secret-key (env :sorcerer-aws-secret-key)
   :sorcerer-dynamodb-endpoint (env :sorcerer-dynamodb-endpoint)
   :sorcerer-pubsub-subscription-name (env :sorcerer-pubsub-subscription-name)
   :sorcerer-k8s-ingress-name (env :sorcerer-k8s-ingress-name)
   :sorcerer-k8s-domain (env :sorcerer-k8s-domain)
   :sorcerer-dns-zone (env :sorcerer-dns-zone)})

(defn -main []
  (component/start
    (sorcerer-system (load-config))))
