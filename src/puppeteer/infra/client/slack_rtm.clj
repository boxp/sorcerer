(ns puppeteer.infra.client.slack-rtm
  (:require [slack-rtm.core :as rtm]
            [clojure.core.async :refer [chan put! close!]]
            [com.stuartsierra.component :as component]))

(defn wrap-for-me?
  [rtm-connection m]
  (-> m
      (assoc :for-me?
             (some->> m
                      :text
                      (re-matches
                        (re-pattern
                          (str "\\<\\@"
                               (-> rtm-connection :start :self :id)
                               "\\> .*")))))))

(defn- subscribe-message
  [rtm-connection]
  (let [c (chan)
        f #(some->> %
                    (wrap-for-me? rtm-connection)
                    (put! c))]
    (rtm/sub-to-event (:events-publication rtm-connection) :message f)
    c))

(defrecord SlackRtmComponent [rtm-connection message-channel token]
  component/Lifecycle
  (start [this]
    (println ";; Starting SlackRtmComponent")
    (let [rtm-connection (rtm/connect token)]
      (-> this
          (assoc :rtm-connection rtm-connection)
          (assoc :message-channel (subscribe-message rtm-connection)))))
  (stop [{:keys [rtm-connection message-channel] :as this}]
    (println ";; Stopping SlackRtmComponent")
    (close! message-channel)
    (when-not (nil? rtm-connection)
      (rtm/send-event (:dispatcher rtm-connection) :close))
    (-> this
        (dissoc :rtm-connection))))

(defn slack-rtm-component
  [token]
  (map->SlackRtmComponent {:token token}))
