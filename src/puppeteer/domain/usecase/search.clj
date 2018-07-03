(ns puppeteer.domain.usecase.search
  (:require [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s]
            [puppeteer.domain.entity.search :as entity]
            [puppeteer.infra.repository.search :as searchrepo]))

(s/def :search-usecase-component/search-repository ::searchrepo/search-repository-component)
(s/def ::search-usecase-component
  (s/keys :opt-un [:search-usecase-component/search-repository]))

(s/fdef search
  :args (s/cat :c :search-usecase-component
               :query string?)
  :ret (s/coll-of ::entity/deployment))
(defn search
  [{:keys [search-repository]} query]
  (searchrepo/search-deployments search-repository query))

(defrecord SearchUsecaseComponent [search-repository]
  component/Lifecycle
  (start [this]
    (println ";; Starting SearchUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping SearchUsecaseComponent")
    this))
