(ns puppeteer.infra.client.dynamodb
  (:import (com.amazonaws ClientConfiguration))
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [difference intersection]]
            [com.stuartsierra.component :as component]
            [taoensso.faraday :as far]))

(s/def ::access-key string?)
(s/def ::secret-key string?)
(s/def ::endpoint string?)
(s/def :dynamodb-component/opts
  (s/keys :req-un [::access-key
                   ::secret-key
                   ::endpoint]))
(s/def ::dynamodb-component
  (s/keys :req-un [::access-key
                   ::secret-key
                   ::endpoint]
          :opt-un [:dynamodb-component/opts]))

(def tables
  {:puppeteer-job [[:id :s]
                   {:throughput {:read 1 :write 1}
                    :block? true}]})

(s/fdef delete-tables
  :args (s/cat :comp ::dynamodb-component)
  :ret true?)
(defn delete-tables
  [{:keys [opts] :as comp}]
  (doseq [t (intersection (-> tables keys set) (-> opts far/list-tables set))]
    (far/delete-table opts t)))

(s/fdef provision-tables
  :args (s/cat :comp ::dynamodb-component)
  :ret true?)
(defn provision-tables
  [{:keys [opts] :as comp}]
  (let [exists-tables (set (far/list-tables opts))]
    (doseq [[k v] tables]
      (if (k exists-tables)
        (apply far/update-table (conj [opts k] (second v)))
        (apply far/create-table (concat [opts k] v))))))

(defrecord DynamoDBComponent [opts access-key secret-key endpoint]
  component/Lifecycle
  (start [{:keys [access-key secret-key endpoint] :as this}]
    (println ";; Starting DynamoDBComponent")
    (let [opts {:access-key access-key
                :secret-key secret-key
                :endpoint endpoint}
          this (assoc this :opts opts)]
      (provision-tables this)
      this))
  (stop [this]
    (println ";; Stopping DynamoDBComponent")
    (dissoc this :opts)))

(s/fdef dynamodb-component
  :args (s/cat :access-key ::access-key
               :secret-key ::secret-key
               :endpoint ::endpoint)
  :ret ::dynamodb-component)
(defn dynamodb-component
  [access-key secret-key endpoint]
  (->DynamoDBComponent {} access-key secret-key endpoint))
