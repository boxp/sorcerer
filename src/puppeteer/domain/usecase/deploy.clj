(ns puppeteer.domain.usecase.deploy
  (:require [com.stuartsierra.component :as component]
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
              (->> % :name keyword (get images)))
      containers)))

(defn- prepare-deployment
  [{:keys [deploy-repository]}
   {:keys [conf build] :as job}]
  (let [deployment (deployrepo/get-resource
                     deploy-repository
                     {:user (:user-name conf)
                      :repo (:repo-name conf)
                      :ref (:branch-name conf)
                      :path (-> conf :k8s :deployment)})
        app (str (:repo-name conf) "-" (:branch-name conf))]
    (-> deployment
        (assoc-in [:metadata :name] app)
        (assoc-in [:spec :template :metadata :labels :app] app)
        (update-in [:spec :template :spec :containers]
                   #(update-containers-image job %)))))

(defn- prepare-service
  [{:keys [deploy-repository]}
   {:keys [conf build] :as job}]
  (let [service (deployrepo/get-resource
                  deploy-repository
                  {:user (:user-name conf)
                   :repo (:repo-name conf)
                   :ref (:branch-name conf)
                   :path (-> conf :k8s :service)})
        app (str (:repo-name conf) "-" (:branch-name conf))]
    (-> service
        (assoc-in [:metadata :name] app)
        (assoc-in [:selector :app] app))))

(defn- prepare-ingress
  [{:keys [deploy-repository]}
   {:keys [] :as job}])

(defn apply
  [{:keys [deploy-repository] :as comp}
   job]
  (let [deployment (prepare-deployment comp job)
        service (prepare-service comp job)]
    (println deployment)
    (println service)
    (deployrepo/apply-resource deploy-repository deployment)
    (deployrepo/apply-resource deploy-repository service)))

(defrecord DeployUsecaseComponent [deploy-repository]
  component/Lifecycle
  (start [this]
    (println ";; Starting DeployUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping DeployUsecaseComponent")
    this))

(defn deploy-usecase-component
  []
  (map->DeployUsecaseComponent {}))
