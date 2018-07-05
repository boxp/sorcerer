(ns sorcerer.infra.client.container-builder
  (:import (com.google.cloud ServiceOptions)
           (com.google.api.services.cloudbuild.v1 CloudBuild CloudBuild$Builder CloudBuild$Projects$Builds$Create CloudBuildRequestInitializer)
           (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)
           (com.google.api.services.cloudbuild.v1.model ListBuildsResponse Results Build)
           (com.google.api.client.googleapis.auth.oauth2 GoogleCredential))
  (:require [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s]))

(s/def ::project-id string?)
(s/def ::client #(instance? CloudBuild %))
(s/def ::access-token string?)
(s/def ::container-builder-client-component
  (s/keys :opt-un [::project-id
                   ::client
                   ::access-token]))

(def application-name "sorcerer")

(s/fdef create-build
  :args (s/cat :comp ::container-builder-client-component
               :build #(instance? Build %))
  :ret true?)
(defn create-build
  [{:keys [project-id client access-token] :as comp} build]
  (-> client
      .projects
      .builds
      (.create project-id build)
      (.setAccessToken
        (.getAccessToken
          (doto (GoogleCredential/getApplicationDefault)
            .refreshToken)))
      .execute))

(defrecord ContainerBuilderClientComponent [project-id client]
  component/Lifecycle
  (start [this]
    (println ";; Starting ContainerBuilderClientComponent")
    (-> this
        (assoc :project-id (ServiceOptions/getDefaultProjectId))
        (assoc :client (-> (CloudBuild$Builder. (GoogleNetHttpTransport/newTrustedTransport)
                                                (JacksonFactory.)
                                                nil)
                           (.setApplicationName application-name)
                           .build))))
  (stop [this]
    (println ";; Stopping ContainerBuilderClientComponent")
    (-> this
        (dissoc :client)
        (dissoc :access-token))))

(s/fdef container-builder-client-component
  :args (s/cat)
  :ret ::container-builder-client-component)
(defn container-builder-client-component
  []
  (map->ContainerBuilderClientComponent {}))
