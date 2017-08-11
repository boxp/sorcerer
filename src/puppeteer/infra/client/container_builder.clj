(ns puppeteer.infra.client.container-builder
  (:import (com.google.cloud ServiceOptions)
           (com.google.api.services.cloudbuild.v1 CloudBuild CloudBuild$Builder CloudBuild$Projects$Builds$Create CloudBuildRequestInitializer)
           (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)
           (com.google.api.services.cloudbuild.v1.model ListBuildsResponse Results Build)
           (com.google.api.client.googleapis.auth.oauth2 GoogleCredential))
  (:require [com.stuartsierra.component :as component]))

(def application-name "puppeteer")

(defn create-build
  [{:keys [project-id client access-token] :as comp} build]
  (println (-> (GoogleCredential/getApplicationDefault) .getAccessToken))
  (-> client
      .projects
      .builds
      (.create project-id build)
      (.setAccessToken
        (-> (GoogleCredential/getApplicationDefault) .getAccessToken))
      .execute))

(defrecord ContainerBuilderClientComponent [project-id client]
  component/Lifecycle
  (start [this]
    (println ";; Starting ContainerBuilderClientComponent")
    (.refreshToken (GoogleCredential/getApplicationDefault))
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

(defn container-builder-client-component
  []
  (map->ContainerBuilderClientComponent {}))
