(ns puppeteer.app.slackbot.alice
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.domain.entity.job :refer [map->Job]]
            [puppeteer.domain.usecase.message :refer [send-deploy-message send-help-message subscribe-message]]
            [puppeteer.domain.usecase.build :refer [build subscribe-build-message]]
            [puppeteer.domain.usecase.conf :refer [load-conf]]
            [puppeteer.domain.usecase.job :refer [get-job set-job]]))

(defn- help
  [{:keys [message-usecase build-usecase]}
   m
   _]
  (send-help-message
    message-usecase
    {:received-message m}))

(defn- deploy
  [{:keys [message-usecase build-usecase conf-usecase job-usecase]}
   m
   [_ _ user-name repo-name branch-name]]
  (as-> (map->Job {}) $
    (assoc $ :conf
           (load-conf
             conf-usecase
             {:user-name user-name
              :repo-name repo-name
              :branch-name branch-name}))
    (assoc $ :message
           (send-deploy-message
             message-usecase
             {:received-message m
              :user-name user-name
              :repo-name repo-name
              :branch-name branch-name}))
    (assoc-in $ [:build :id]
           (build
             build-usecase
             {:conf (:conf $)
              :user-name user-name
              :repo-name repo-name
              :branch-name branch-name}))
    (set-job job-usecase $)))

(defn- deploy-succeed
  [{:keys [message-usecase build-usecase conf-usecase job-usecase]}
   m]
  (as-> (get-job job-usecase (:id m)) $
        (println $)))

(defmulti reaction
  (fn [_ m] (:type m)))

(defmethod reaction :message
  [{:keys [message-usecase build-usecase] :as comp}
   m]
  (let [txt (some-> m :text (clojure.string/split #" "))
        [_ command _] txt]
    (if (:for-me? m)
      (case command
        "deploy" (async/go (deploy comp m txt))
        (help comp m txt)))))

(defmethod reaction :build
  [{:keys [message-usecase build-usecase] :as comp}
   m]
  (case (:status m)
    "SUCCESS" (deploy-succeed comp m)
    "FAILURE" nil
    nil))

(defmethod reaction :default
  [_ _])

(defn alice-loop
  [{:keys [message-usecase build-usecase] :as comp}]
  (let [message-chan (subscribe-message message-usecase)
        build-chan (subscribe-build-message build-usecase)]
    (async/go-loop [m {}]
      (when m
        (reaction comp m)
        (recur (async/alt! message-chan
                           ([v _] (some-> v (assoc :type :message)))
                           build-chan
                           ([v _] (some-> v (assoc :type :build)))))))))

(defrecord AliceComponent [message-usecase build-usecase conf-usecase job-usecase]
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
