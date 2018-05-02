(ns puppeteer.infra.client.cloud-datastore
(:require [com.stuartsierra.component :as component]
          [clojure.spec.alpha :as s])
(:import (com.google.cloud.datastore Entity Key KeyFactory DatastoreOptions)))

(s/fdef put-entity
  :args (s/cat :c map?
               :kind string?
               :key string?
               :entity-map (s/map-of string? any?))
  :ret true?)
(defn put-entity
  [{:keys [client] :as c}
   kind key entity-map]
  (let [entity (.. (Entity/newBuilder
                     (.. client
                         newKeyFactory
                         (setKind kind)))
                   (newKey key))]
    (doseq [[k v] entity-map]
      (.set entity k v))
    (.build entity)
    (.put client entity)))


(defrecord CloudDatastoreComponent [client]
  component/Lifecycle
  (start [this]
    (println ";; Starting CloudDatastoreComponent")
    (-> this
        (assoc :client (.. DatastoreOptions
                           getDefaultInstance
                           getService))))
  (stop [this]
    (println ";; Stopping CloudDatastoreComponent")
    (-> this
        (dissoc :client))))
