(ns sorcerer.infra.repository.conf
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [sorcerer.infra.client.github :as github]
            [sorcerer.domain.entity.conf :as conf-entity]
            [sorcerer.domain.entity.build :as build-entity]))

(s/def ::content #(instance? (-> "" .getBytes class) %))
(s/def :configuration-repository-component/github-client ::github/github-component)
(s/def ::configuration-repository-component
  (s/keys :opt-un [:configuration-repository-component/github-client]))

(s/fdef Content->Configuration
  :args (s/cat :content ::content)
  :ret ::conf-entity/configuration)
(defn- Content->Configuration
  [content]
  (some->> content
           slurp
           read-string))

(def conf-path "puppet.edn")

(s/fdef load-conf
  :args (s/cat :c ::configuration-repository-component
               :opts (s/keys :req-un [::build-entity/user-name
                                      ::build-entity/repo-name
                                      ::build-entity/branch-name]))
  :ret (s/nilable ::conf-entity/configuration))
(defn load-conf
  [{:keys [github-client] :as c}
   {:keys [user-name repo-name branch-name] :as configuration}]
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

(s/fdef configuration-repository-component
  :args (s/cat)
  :ret ::configuration-repository-component)
(defn configuration-repository-component
  []
  (map->ConfigurationRepositoryComponent {}))
