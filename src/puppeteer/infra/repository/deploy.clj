(ns puppeteer.infra.repository.deploy
  (:require [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [com.stuartsierra.component :as component]
            [puppeteer.infra.client.github :as github])
  (:import (com.google.api.services.dns.model Change ResourceRecordSet)))

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
      (.inNamespace "default")
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
      (.inNamespace "default")
      .createOrReplace))

(defn delete-resource [{:keys [k8s-client] :as comp}
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
      (.withName app)))

(defn add-subdomain
  [{:keys [cloud-dns-client domain] :as comp}
   {:keys [repo-name branch-name]}]
  (try
    (let [resource-record-set (-> (ResourceRecordSet.)
                                  (.setKind "dns#resourceRecordSet")
                                  (.setType "CNAME")
                                  (.setName (str repo-name "-" branch-name "." domain "."))
                                  (.setTtl (int 300))
                                  (.setRrdatas [(str domain ".")]))
          change (-> (Change.)
                     (.setKind "dns#change")
                     (.setAdditions [resource-record-set]))]
      (-> (:client cloud-dns-client)
          .changes
          (.create (:project-id cloud-dns-client) (:dns-zone cloud-dns-client) change)
          .execute))
    (catch Exception e
      (case (.getStatusCode e)
        409 nil
        (throw e)))))

(defn remove-subdomain
  [{:keys [cloud-dns-client domain] :as comp}
   {:keys [repo-name branch-name]}]
  (let [resource-record-set (-> (ResourceRecordSet.)
                                (.setKind "dns#resourceRecordSet")
                                (.setType "CNAME")
                                (.setName (str repo-name "-" branch-name "." domain "."))
                                (.setTtl (int 300))
                                (.setRrdatas [(str domain ".")]))
        change (-> (Change.)
                   (.setKind "dns#change")
                   (.setDeletions [resource-record-set]))]
    (-> (:client cloud-dns-client)
        .changes
        (.create (:project-id cloud-dns-client) (:dns-zone cloud-dns-client) change)
        .execute)))

(defrecord DeployRepositoryComponent [k8s-client github-client cloud-dns-client domain ingress-name]
  component/Lifecycle
  (start [this]
    (println ";; Starting DeployRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping DeployRepositoryComponent")
    this))

(defn deploy-repository-component
  [domain ingress-name]
  (map->DeployRepositoryComponent {:domain domain
                                   :ingress-name ingress-name}))
