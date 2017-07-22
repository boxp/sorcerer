(ns puppeteer.infra.client.slack
  (:require [clj-slack.core :as slack]
            [clj-slack.chat :as chat]
            [com.stuartsierra.component :as component]))

(def api-url "https://slack.com/api")

(defn connect
  [token]
  {:connection {:token token
                :api-url api-url}})
(defn post
  [{:keys [connection]} {:keys [channel text optionals]}]
  (chat/post-message connection channel text (merge {:as_user "true"} optionals)))

(defn reply
  [{:keys [connection]} {:keys [channel text user optionals]}]
  (chat/post-message connection channel (str "<@" user "> " text) (merge {:as_user "true"} optionals)))

(defn update
  [{:keys [connection]} {:keys [channel ts text optionals]}]
  (slack/slack-request connection
                       "chat.update"
                       {"ts" ts
                        "channel" channel
                        "text" text
                        "attachments" (:attachments optionals)
                        "as_user" true}))

(defrecord SlackComponent [connection token]
  component/Lifecycle
  (start [this]
    (println ";; Starting SlackComponent")
    (-> this
        (assoc :connection (connect token))))
  (stop [this]
    (println ";; Stopping SlackComponent")
    (-> this
        (dissoc :connection))))

(defn slack-component
  [token]
  (map->SlackComponent {:token token}))
