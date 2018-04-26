(ns puppeteer.infra.client.slack
  (:require [clojure.spec.alpha :as s]
            [clj-slack.core :as slack]
            [clj-slack.chat :as chat]
            [com.stuartsierra.component :as component]))

(s/def ::token string?)
(s/def ::api-url string?)
(s/def ::connection
  (s/nilable
    (s/keys :req-un [::api-url
                     ::token])))
(s/def ::channel string?)
(s/def ::text string?)
(s/def ::ts string?)
(s/def ::user string?)
(s/def ::optionals map?)
(s/def ::slack-component
  (s/keys :req-un [::token]
          :opt-un [::connection]))

(def api-url "https://slack.com/api")

(s/fdef connect
  :args (s/cat :token ::token)
  :ret ::connection)
(defn connect
  [token]
  {:token token
   :api-url api-url})

(s/fdef post
  :args (s/cat :c ::slack-component
               :opts (s/keys :req-un [::channel
                                      ::text
                                      ::optionals]))
  :ret true?)
(defn post
  [{:keys [connection] :as c} {:keys [channel text optionals] :as opts}]
  (chat/post-message connection channel text (merge {:as_user "true"} optionals)))

(s/fdef reply
  :args (s/cat :c ::slack-component
               :opts (s/keys :req-un [::channel
                                      ::text
                                      ::user
                                      ::optionals]))
  :ret true?)
(defn reply
  [{:keys [connection] :as c} {:keys [channel text user optionals] :as opts}]
  (chat/post-message connection channel (str "<@" user "> " text) (merge {:as_user "true"} optionals)))

(s/fdef update
  :args (s/cat :c ::slack-component
               :opts (s/keys :req-un [::channel
                                      ::text
                                      ::user
                                      ::ts
                                      ::optionals]))
  :ret true?)
(defn update
  [{:keys [connection] :as c} {:keys [channel ts text optionals] :as opts}]
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

(s/fdef slack-component
  :args (s/cat :token ::token)
  :ret ::slack-component)
(defn slack-component
  [token]
  (map->SlackComponent {:token token}))
