(ns sorcerer.infra.client.cloud-dns
  (:require [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s])
  (:import (com.google.cloud ServiceOptions)
           (com.google.api.client.googleapis.auth.oauth2 GoogleCredential)
           (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)
           (com.google.api.services.dns Dns Dns$Builder)))

(s/def ::client #(instance? Dns %))
(s/def ::project-id string?)
(s/def ::dns-zone string?)
(s/def ::cloud-dns-component
  (s/keys :req-un [::client ::project-id ::dns-zone]))

(def application-name
  "sorcerer")

(s/fdef cloud-dns
  :args (s/cat :dns-zone ::dns-zone)
  :ret ::client)
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

(s/fdef cloud-dns-component
  :args (s/cat :dns-zone ::dns-zone)
  :ret ::cloud-dns-component)
(defn cloud-dns-component
  [dns-zone]
  (map->CloudDnsComponent {:dns-zone dns-zone}))
