(ns puppeteer.infra.repository.conf
  (:require [com.stuartsierra.component :as component]
            [puppeteer.infra.client.github :as github]
            [puppeteer.domain.entity.conf :refer [map->Configuration]]))

(defn- Content->Configuration
  [content]
  (some->> content
           read-string
           map->Configuration))

(def conf-path "puppet.edn")

(defn load-conf
  [{:keys [github-client]}
   {:keys [user-name repo-name branch-name]}]
  (some->>
    (github/get-file github-client
                     {:user user-name
                      :repo repo-name
                      :path conf-path
                      :ref branch-name})
    Content->Configuration))

(defrecord ConfigurationRepositoryComponent [github-client]
  component/Lifecycle
  (start [this]
    (println ";; Starting ConfigurationRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping ConfigurationRepositoryComponent")
    this))

(defn configuration-repository-component
  []
  (map->ConfigurationRepositoryComponent {}))
