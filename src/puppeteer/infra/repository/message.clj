(ns puppeteer.infra.repository.message
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.infra.client.slack :as slack-cli]
            [puppeteer.infra.client.slack-rtm :as slack-rtm]
            [puppeteer.domain.entity.message :refer [map->Message]]))

(defn- Message->Post
  [{:keys [channel-id text attachments]}]
  {:channel channel-id
   :text text
   :optionals {:attachments attachments}})

(defn- Post->Message
  [{:keys [channel user ts for-me? optionals]}]
  (map->Message {:channel-id channel
                 :user-id user
                 :for-me? for-me?
                 :timestamp ts
                 :attachments (:attachments optionals)}))

(defn- Message->Reply
  [{:keys [channel-id user-id text attachments]}]
  {:channel channel-id
   :user-id user-id
   :text text
   :optionals {:attachments attachments}})

(defn- Message->Update
  [{:keys [channel-id text timestamp attachments]}]
  {:channel channel-id
   :text text
   :ts timestamp
   :optionals {:attachments attachments}})

(defn subscribe-message
  [{:keys [slack-rtm-client]}]
  (->> (:message-channel slack-rtm-client)
       (async/map Post->Message)))

(defn send-message
  [{:keys [slack-client]} message]
  (if (:user-id message)
    (->> message
         Message->Reply
         (slack-cli/reply slack-client))
    (->> message
         Message->Post
         (slack-cli/post slack-client))))

(defn update-message
  [{:keys [slack-client]} message]
  (->> message
       Message->Update
       (slack-cli/update slack-client)))

(defrecord MessageRepositoryComponent [slack-rtm-client slack-client]
  component/Lifecycle
  (start [{:keys [slack-client slack-rtm-client] :as this}]
    (println ";; Starting MessageRepositoryComponent")
    this)
  (stop [this]
    (println ";; Stopping MessageRepositoryComponent")
    this))

(defn message-repository-component
  []
  (map->MessageRepositoryComponent {}))
