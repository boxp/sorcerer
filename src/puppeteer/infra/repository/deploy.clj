(ns puppeteer.infra.repository.deploy
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [com.stuartsierra.component :as component]
            [puppeteer.infra.client.github :as github]
            [puppeteer.infra.client.k8s :as k8s]
            [puppeteer.infra.client.cloud-dns :as cloud-dns])
  (:import (io.fabric8.kubernetes.api.model.extensions Ingress)
           (com.google.api.services.dns.model Change ResourceRecordSet)))

(s/def ::domain string?)
(s/def ::ingress-name string?)
(s/def ::user string?)
(s/def ::repo string?)
(s/def ::ref string?)
(s/def ::path string?)
(s/def :deploy-repository-component/k8s-client ::k8s/k8s-client-component)
(s/def :deploy-repository-component/github-client ::github/github-component)
(s/def :deploy-repository-component/cloud-dns-client ::cloud-dns/cloud-dns-component)
(s/def ::deploy-repository-component
  (s/keys :req-un [::domain ::ingress-name]
          :opt-un [:deploy-repository-component/k8s-client
                   :deploy-repository-component/github-client
                   :deploy-repository-component/cloud-dns-client]))

(s/fdef get-ingress
  :args (s/cat :comp ::deploy-repository-component)
  :ret Ingress)
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

(s/fdef apply-ingress
  :args (s/cat :comp ::deploy-repository-component
               :resource Ingress)
  :ret true?)
(defn apply-ingress
  [{:keys [k8s-client ingress-name] :as comp}
   resource]
  (-> k8s-client
      :client
      (.resource resource)
      (.inNamespace "default")
      .apply))

(s/fdef get-resource
  :args (s/cat :comp ::deploy-repository-component
               :opts (s/keys :req-un [::user
                                      ::repo
                                      ::ref
                                      ::path]))
  :ret map?)
(defn get-resource
  [{:keys [github-client] :as comp}
   {:keys [user repo ref path] :as :opts}]
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

(defn delete-deployment
  [{:keys [k8s-client] :as comp}
   {:keys [app]}]
  (-> k8s-client
      :client
      .extensions
      .deployments
      (.inNamespace "default")
      (.withName app)
      .delete))

(defn delete-service
  [{:keys [k8s-client] :as comp}
   {:keys [app]}]
  (-> k8s-client
      :client
      .services
      (.inNamespace "default")
      (.withName app)
      .delete))

(defn add-subdomain
  [{:keys [cloud-dns-client domain] :as comp}
   {:keys [host]}]
  (try
    (let [resource-record-set (-> (ResourceRecordSet.)
                                  (.setKind "dns#resourceRecordSet")
                                  (.setType "CNAME")
                                  (.setName (str host "."))
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
   {:keys [host]}]
  (let [resource-record-set (-> (ResourceRecordSet.)
                                (.setKind "dns#resourceRecordSet")
                                (.setType "CNAME")
                                (.setName (str host "."))
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
