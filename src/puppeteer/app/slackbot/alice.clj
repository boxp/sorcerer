(ns puppeteer.app.slackbot.alice
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.domain.usecase.message :refer [send-build-message subscribe-message]]
            [puppeteer.domain.usecase.build :refer []]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]))

(defn- build
  [{:keys [message-usecase build-usecase]}
   m
   _]
  (send-build-message message-usecase {:received-message m
                                       :repo-name "eure-atnd"
                                       :branch-name "master"}))

(defn reaction
  [{:keys [message-usecase build-usecase]}
   m]
  (let [txt (-> m (clojure.string/split #" "))
        [_ command _] txt]
    (throw m)
    (if (:for-me? m)
      (case command
        "build" (build m txt)))))

(defn alice-loop
  [{:keys [message-usecase build-usecase]}]
  (let [c (subscribe-message message-usecase)]
    (async/go-loop [m (async/<! c)]
      (when m
        (reaction m)
        (recur (async/<! c))))))

(defrecord AliceComponent [message-usecase build-usecase]
  component/Lifecycle
  (start [{:keys [message-usecase build-usecase] :as this}]
    (println ";; Starting AliceComponent")
    this)
  (stop [this]
    (println ";; Stopping AliceComponent")
    this))

(defn alice-component
  []
  (map->AliceComponent {}))
