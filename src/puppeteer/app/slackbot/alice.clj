(ns puppeteer.app.slackbot.alice
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.domain.usecase.message :refer [send-deploy-message send-help-message subscribe-message]]
            [puppeteer.domain.usecase.build :refer []]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]))

(defn- help
  [{:keys [message-usecase build-usecase]}
   m
   _]
  (send-help-message
    message-usecase
    {:received-message m}))

(defn- deploy
  [{:keys [message-usecase build-usecase]}
   m
   [_ _ repo-name branch-name]]
  (println
    (send-deploy-message
      message-usecase
      {:received-message m
       :repo-name repo-name
       :branch-name branch-name})))

(defn reaction
  [{:keys [message-usecase build-usecase] :as comp}
   m]
  (let [txt (some-> m :text (clojure.string/split #" "))
        [_ command _] txt]
    (if (:for-me? m)
      (case command
        "deploy" (deploy comp m txt)
        (help comp m txt)))))

(defn alice-loop
  [{:keys [message-usecase build-usecase] :as comp}]
  (let [c (subscribe-message message-usecase)]
    (async/go-loop [m (async/<! c)]
      (when m
        (reaction comp m)
        (recur (async/<! c))))))

(defrecord AliceComponent [message-usecase build-usecase]
  component/Lifecycle
  (start [{:keys [message-usecase build-usecase] :as this}]
    (println ";; Starting AliceComponent")
    (alice-loop this)
    this)
  (stop [this]
    (println ";; Stopping AliceComponent")
    this))

(defn alice-component
  []
  (map->AliceComponent {}))
