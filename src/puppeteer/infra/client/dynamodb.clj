(ns puppeteer.infra.client.dynamodb
  (:import (com.amazonaws ClientConfiguration))
  (:require [clojure.set :refer [difference intersection]]
            [com.stuartsierra.component :as component]
            [taoensso.faraday :as far]))

(def tables
  {:puppeteer-job [[:id :s]
                   {:throughput {:read 1 :write 1}
                    :block? true}]})

(defn delete-tables
  [{:keys [opts] :as comp}]
  (doseq [t (intersection (-> tables keys set) (-> opts far/list-tables set))]
    (far/delete-table opts t)))

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

(defn dynamodb-component
  [access-key secret-key endpoint]
  (->DynamoDBComponent {} access-key secret-key endpoint))
