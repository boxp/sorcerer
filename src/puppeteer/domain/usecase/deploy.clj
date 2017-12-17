(ns puppeteer.domain.usecase.deploy
  (:import (io.fabric8.kubernetes.api.model IntOrString)
           (io.fabric8.kubernetes.api.model.extensions IngressRule HTTPIngressRuleValue HTTPIngressPath IngressBackend))
  (:require [puppeteer.util :refer [->app ->host ->service-name]]
            [com.stuartsierra.component :as component]
            [flatland.ordered.map :refer [ordered-map]]
            [puppeteer.infra.repository.deploy :as deployrepo]))

(defn- update-containers-image
  [{:keys [conf build] :as job} containers]
  (let [images (into (ordered-map)
                     (map
                       (fn [[image-name _]
                            image]
                         [image-name image])
                       (:images conf)
                       (:images build)))]
    (map
      #(assoc % :image
              (or
                (->> % :name keyword (get images))
                (:image %)))
      containers)))

(defn- prepare-deployment
  [{:keys [deploy-repository]}
   {:keys [conf build user-name repo-name branch-name] :as job}]
  (let [deployment (deployrepo/get-resource
                     deploy-repository
                     {:user user-name
                      :repo repo-name
                      :ref branch-name
                      :path (-> conf :k8s :deployment)})
        app (->app job)]
    (-> deployment
        (assoc-in [:metadata :name] app)
        (assoc-in [:spec :template :metadata :labels :app] app)
        (assoc-in [:spec :replicas] 1)
        (update-in [:spec :template :spec :containers]
                   #(update-containers-image job %)))))

(defn- prepare-service
  [{:keys [deploy-repository]}
   {:keys [conf build user-name repo-name branch-name] :as job}]
  (let [service (deployrepo/get-resource
                  deploy-repository
                  {:user user-name
                   :repo repo-name
                   :ref branch-name
                   :path (-> conf :k8s :service)})
        app (->app job)]
    (-> service
        (assoc-in [:metadata :name] app)
        (assoc-in [:spec :selector :app] app))))

(defn- prepare-ingress
  [{:keys [deploy-repository domain]}
   {:keys [conf build user-name repo-name branch-name] :as job}]
  (let [host (->host {:domain domain
                      :repo-name repo-name
                      :branch-name branch-name})
        service-name (->service-name job)
        ingress (deployrepo/get-ingress deploy-repository)
        spec (.getSpec ingress)
        tls (.getTls spec)
        rules (.getRules spec)]
    (.setSpec ingress
      (doto spec
          (.setTls
            (map
              (fn [tls]
                (let [hosts (.getHosts tls)]
                  (doto tls
                    (.setHosts
                      (if (some #(= % host) hosts)
                        hosts
                        (doto hosts
                          (.add host)))))))
              tls))
          (.setRules
            (if (some #(= (:host %) host) rules)
              rules
              (doto rules
                (.add
                  (IngressRule. host
                    (HTTPIngressRuleValue.
                      [(HTTPIngressPath.
                         (IngressBackend.
                           service-name
                           (-> 80 int IntOrString.))
                         "/*")]))))))))
    ingress))

(defn apply
  [{:keys [domain deploy-repository] :as comp}
   {:keys [repo-name branch-name] :as job}]
  (let [deployment (prepare-deployment comp job)
        service (prepare-service comp job)
        ingress (prepare-ingress comp job)
        host (->host {:domain domain
                      :repo-name repo-name
                      :branch-name branch-name})]
    (deployrepo/apply-resource deploy-repository {:k8s deployment})
    (deployrepo/apply-resource deploy-repository {:k8s service})
    (deployrepo/apply-ingress deploy-repository ingress)
    (deployrepo/add-subdomain deploy-repository {:host host})
    {:host host}))

(defn round-up
  [{:keys [domain deploy-repository] :as comp}
   {:keys [repo-name branch-name] :as job}]
  (let [app (->app job)]
    (deployrepo/delete-service deploy-repository {:app app})
    (deployrepo/delete-deployment deploy-repository {:app app})
    (deployrepo/remove-subdomain deploy-repository
                                 {:host
                                  (->host {:domain domain
                                           :repo-name repo-name
                                           :branch-name branch-name})})))

(defrecord DeployUsecaseComponent [deploy-repository domain]
  component/Lifecycle
  (start [this]
    (println ";; Starting DeployUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping DeployUsecaseComponent")
    this))

(defn deploy-usecase-component
  [domain]
  (map->DeployUsecaseComponent {:domain domain}))
