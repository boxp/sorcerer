(ns sorcerer.infra.client.github
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [tentacles.core :as core]
            [tentacles.repos :as repos]))

(s/def ::github-oauth-token string?)
(s/def ::user string?)
(s/def ::repo string?)
(s/def ::ref string?)
(s/def ::path string?)
(s/def :base-opt/oauth-token ::github-oauth-token)
(s/def ::base-opt
  (s/keys :req-un [:base-opt/oauth-token]))
(s/def ::github-component
  (s/keys :req-un [::github-oauth-token]
          :opt-un [::base-opt]))

(s/fdef get-file
  :args (s/cat :c ::github-component
               :opts (s/keys :req-un [::user ::repo ::ref ::path]))
  :ret #(instance? (-> "" .getBytes class) %))
(defn get-file
  [{:keys [base-opt] :as c}
   {:keys [user repo ref path] :as opts}]
  (some->> (assoc base-opt :ref ref)
           (repos/contents user repo path)
           :content))

(s/def :get-tarball-response/body string?)
(s/fdef get-tarball
  :args (s/cat :c ::github-component
               :opts (s/keys :req-un [::user ::repo ::ref]))
  :ret (s/keys :req-un [:get-tarball-response/body]))
(defn get-tarball
  [{:keys [base-opt] :as c}
   {:keys [user repo ref] :as opts}]
  (core/raw-api-call :get "repos/%s/%s/tarball/%s"
                     [user repo ref]
                     base-opt))

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

(s/fdef github-component
  :args (s/cat :github-oauth-token ::github-oauth-token)
  :ret ::github-component)
(defn github-component
  [github-oauth-token]
  (map->GithubComponent {:github-oauth-token github-oauth-token}))
