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
        app (str repo-name "-" branch-name)]
    (-> deployment
        (assoc-in [:metadata :name] app)
        (assoc-in [:spec :template :metadata :labels :app] app)
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
        app (str repo-name "-" branch-name)]
    (-> service
        (assoc-in [:metadata :name] app)
        (assoc-in [:selector :app] app))))

(defn- prepare-ingress
  [{:keys [deploy-repository]}
   {:keys [conf build user-name repo-name branch-name] :as job}]
  (let [ingress (deployrepo/get-ingress deploy-repository)]
    ingress))

(defn apply
  [{:keys [deploy-repository] :as comp}
   job]
  (let [deployment (prepare-deployment comp job)
        service (prepare-service comp job)]
    (->> deployment println)
    (->> service println)
    (deployrepo/apply-resource deploy-repository {:k8s deployment})
    (deployrepo/apply-resource deploy-repository {:k8s service})))

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
