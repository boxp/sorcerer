(ns puppeteer.app.slackbot.alice
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.domain.entity.job :refer [map->Job]]
            [puppeteer.domain.usecase.message :as message-usecase]
            [puppeteer.domain.usecase.build :refer [build subscribe-build-message]]
            [puppeteer.domain.usecase.conf :refer [load-conf]]
            [puppeteer.domain.usecase.job :refer [get-job set-job]]
            [puppeteer.domain.usecase.deploy :refer [apply]]))

(defn- help
  [{:keys [message-usecase build-usecase]}
   m
   _]
  (message-usecase/send-help-message
    message-usecase
    {:message m}))

(defn- deploy
  [{:keys [message-usecase build-usecase conf-usecase job-usecase]}
   m
   [_ _ user-name repo-name branch-name]]
  (as-> (map->Job {:user-name user-name
                   :repo-name repo-name
                   :branch-name branch-name
                   :message m}) $
    (assoc $ :conf (load-conf conf-usecase $))
    (assoc-in $ [:build :id] (build build-usecase $))
    (do (message-usecase/send-deploy-start-message message-usecase $) $)
    (set-job job-usecase $)))

(defn- build-succeed
  [{:keys [message-usecase build-usecase conf-usecase job-usecase deploy-usecase]}
   m]
  (let [job (get-job job-usecase (:id m))]
    (when job
      (as-> job $
	(assoc $ :build m)
        (do (message-usecase/send-build-succeed-message message-usecase $) $)
        (do (message-usecase/send-deploy-start-message message-usecase $) $)
        (do (apply deploy-usecase $) $)
        (do (message-usecase/send-deploy-succeed-message message-usecase $) $)))))

(defn- build-failure
  [{:keys [message-usecase build-usecase conf-usecase job-usecase]}
   m]
  (let [job (get-job job-usecase (:id m))]
    (when job
      (message-usecase/send-build-failure-message message-usecase job))))

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
    "SUCCESS" (build-succeed comp m)
    "FAILURE" (build-failure comp m)
    (println m)))

(defmethod reaction :default
  [_ _])

(defn alice-loop
  [{:keys [message-usecase build-usecase] :as comp}]
  (let [message-chan (message-usecase/subscribe-message message-usecase)
        build-chan (subscribe-build-message build-usecase)]
    (async/go-loop [m {}]
      (when m
        (reaction comp m)
        (recur (async/alt! message-chan
                           ([v _] (some-> v (assoc :type :message)))
                           build-chan
                           ([v _] (some-> v (assoc :type :build)))))))))

(defrecord AliceComponent [message-usecase build-usecase conf-usecase job-usecase deploy-usecase]
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
