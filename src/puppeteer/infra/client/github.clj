(ns puppeteer.infra.client.github
  (:require [com.stuartsierra.component :as component]
            [tentacles.core :as core]
            [tentacles.repos :as repos]))

(defn get-file
  [{:keys [base-opt]}
   {:keys [user repo path]}]
  (some->> (repos/contents user repo path base-opt)
           :content
           slurp))

(defrecord GithubComponent [github-oauth-token base-opt]
  component/Lifecycle
  (start [{:keys [github-oauth-token] :as this}]
    (println ";; Starting GithubComponent")
    (let [base-opt {:oauth-token github-oauth-token}]
      (-> this
          (assoc :base-opt base-opt))))
  (stop [this]
    (println ";; Stopping GithubComponent")
    (-> this
        (dissoc :base-opt))))

(defn github-component
  [github-oauth-token]
  (map->GithubComponent {:github-oauth-token github-oauth-token}))
