(ns puppeteer.infra.client.slack-rtm
  (:require [slack-rtm.core :as rtm]
            [clojure.core.async :refer [chan put!]]
            [com.stuartsierra.component :as component]))

(defn wrap-for-me?
  [rtm-connection m]
  (assoc m :for-me?
         (some->> m
                  :txt
                  (re-matches
                    (re-pattern
                      (str "\\<\\@"
                           (-> rtm-connection :start :self :id)
                           "\\> .*"))))))

(defn subscribe-message
  [rtm-connection]
  (let [c (chan)
        f #(some->> %
                    (wrap-for-me? rtm-connection)
                    (put! c))]
    (rtm/sub-to-event (:events-publication rtm-connection) :message f)))

(defrecord SlackRtmComponent [rtm-connection token]
  component/Lifecycle
  (start [this]
    (println ";; Starting SlackRtmComponent")
    (-> this
        (assoc :rtm-connection (rtm/connect token))))
  (stop [this]
    (println ";; Stopping SlackRtmComponent")
    (-> this
        (dissoc :rtm-connection))))

(defn slack-rtm-component
  [token]
  (map->SlackRtmComponent {:token token}))
