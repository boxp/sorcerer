(ns puppeteer.infra.client.slack-rtm
  (:require [clojure.spec.alpha :as s]
            [slack-rtm.core :as rtm]
            [clojure.core.async :refer [chan put! close!]]
            [com.stuartsierra.component :as component]))

(s/def ::rtm-connection (s/nilable map?))
(s/def ::message-channel (s/nilable #(instance? (-> (chan) class) %)))
(s/def ::token string?)
(s/def ::text string?)
(s/def ::user string?)
(s/def ::for-me? boolean?)
(s/def ::from-me? boolean?)
(s/def ::slack-rtm-component
  (s/keys :req-un [::token]
          :opt-un [::rtm-connection
                   ::message-channel]))

(s/fdef wrap-for-me?
  :args (s/cat :rtm-connection ::rtm-connection
               :m (s/keys :req-un [::text]))
  :ret (s/keys :req-un [::for-me?]))
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


(s/fdef wrap-from-me?
  :args (s/cat :rtm-connection ::rtm-connection
               :m (s/keys :req-un [::user]))
  :ret (s/keys :req-un [::from-me?]))
(defn wrap-from-me?
  [rtm-connection m]
  (-> m
      (assoc :from-me?
             (some->> m
                      :user
                      (= (-> rtm-connection :start :self :id))))))

(s/fdef subscribe-message
  :args (s/cat :rtm-connection ::rtm-connection)
  :ret ::message-channel)
(defn- subscribe-message
  [rtm-connection]
  (let [c (chan)
        f #(some->> %
                    (wrap-for-me? rtm-connection)
                    (wrap-from-me? rtm-connection)
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

(s/fdef slack-rtm-component
  :args (s/cat :token ::token)
  :ret ::slack-rtm-component)
(defn slack-rtm-component
  [token]
  (map->SlackRtmComponent {:token token}))
