(ns puppeteer.infra.repository.deploy
  (:require [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [com.stuartsierra.component :as component]
            [puppeteer.infra.client.github :as github]))

(defn get-ingress
  [{:keys [k8s-client ingress-name] :as comp}]
  (-> k8s-client
      :client
      .extensions
      .ingresses
      (.withName ingress-name)
      .list
      .getItems
      first))

(defn apply-ingress
  [{:keys [k8s-client ingress-name] :as comp}
   resource]
  (-> k8s-client
      :client
      (.resource resource)
      .apply))

(defn get-resource
  [{:keys [github-client] :as comp}
   {:keys [user repo ref path]}]
  (some-> (github/get-file
            github-client
            {:user user
             :repo repo
             :ref ref
             :path path})
          slurp
          (yaml/parse-string :keywords true)))

(defn apply-resource
  [{:keys [k8s-client] :as comp}
   {:keys [k8s]}]
  (-> k8s-client
      :client
      (.load (-> k8s
                 yaml/generate-string
                 .getBytes
                 io/input-stream))
      .createOrReplace))

(defn delete-resource
  [{:keys [k8s-client] :as comp}
   {:keys [resource]}]
  (-> k8s-client
      :client
      (.load (-> resource
                 yaml/generate-string
                 .getBytes
                 io/input-stream))
      .delete))

(defn delete-service
  [{:keys [k8s-client] :as comp}
   {:keys [app]}]
  (-> k8s-client
      :client
      .services
      (.withName "eure-atnd")))

(defrecord DeployRepositoryComponent [k8s-client github-client domain ingress-name]
  component/Lifecycle
  (start [this]
    (println ";; Starting BuildRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping BuildRepositoryComponent")
    this))

(defn deploy-repository-component
  [domain ingress-name]
  (map->DeployRepositoryComponent {:domain domain
                                   :ingress-name ingress-name}))
