(ns sorcerer.infra.repository.message
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [sorcerer.infra.client.slack :as slack-cli]
            [sorcerer.infra.client.slack-rtm :as slack-rtm]
            [sorcerer.domain.entity.message :refer [map->Message]]))

(defn- Message->Post
  [{:keys [channel-id text attachments]}]
  {:channel channel-id
   :text text
   :optionals (if attachments {:attachments attachments})})

(defn- Post->Message
  [{:keys [channel user text ts for-me? from-me? optionals] :as post}]
  (map->Message {:channel-id channel
                 :user-id user
                 :text text
                 :for-me? for-me?
                 :from-me? from-me?
                 :timestamp ts
                 :attachments (:attachments optionals)}))

(defn- Message->Reply
  [{:keys [channel-id user-id text attachments]}]
  {:channel channel-id
   :user user-id
   :text text
   :optionals (if attachments {:attachments attachments})})

(defn- Message->Update
  [{:keys [channel-id text timestamp attachments]}]
  {:channel channel-id
   :text text
   :ts timestamp
   :optionals (if attachments {:attachments attachments})})

(defn- PostResult->Message
  [{:keys [ts channel text message] :as res}]
  {:channel-id channel
   :text (:text message)
   :timestamp ts
   :attachments (:attachments message)})

(defn subscribe-message
  [{:keys [slack-rtm-client]}]
  (->> [(:message-channel slack-rtm-client)]
       (async/map Post->Message)))

(defn send-message
  [{:keys [slack-client]} message]
  (if (:user-id message)
    (->> message
         Message->Reply
         (slack-cli/reply slack-client)
         PostResult->Message)
    (->> message
         Message->Post
         (slack-cli/post slack-client)
         PostResult->Message)))

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
