(ns puppeteer.infra.client.cloud-dns
  (:require [com.stuartsierra.component :as component])
  (:import (com.google.cloud ServiceOptions)
           (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
           (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)
           (com.google.api.services.dns Dns$Builder)))

(def application-name
  "puppeteer")

(defn cloud-dns
  [dns-zone]
  (let [http-transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (JacksonFactory/getDefaultInstance)
        credential (GoogleCredential/getApplicationDefault)]
    (-> (Dns$Builder. http-transport json-factory credential)
        (.setApplicationName application-name)
        .build)))

(defrecord CloudDnsComponent [client project-id dns-zone]
  component/Lifecycle
  (start [this]
    (println ";; Starting CloudDnsComponent...")
    (-> this
        (assoc :project-id (ServiceOptions/getDefaultProjectId))
        (assoc :client (cloud-dns dns-zone))))
  (stop [this]
    (println ";; Stopping CloudDnsComponent...")
    (-> this
        (dissoc :client))))

(defn cloud-dns-component
  [dns-zone]
  (map->CloudDnsComponent {:dns-zone dns-zone}))
