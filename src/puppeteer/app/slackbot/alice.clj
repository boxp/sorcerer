(ns puppeteer.app.slackbot.alice
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.domain.entity.job :refer [map->Job]]
            [puppeteer.domain.usecase.message :as message-usecase]
            [puppeteer.domain.usecase.build :refer [build subscribe-build-message]]
            [puppeteer.domain.usecase.conf :refer [load-conf]]
            [puppeteer.domain.usecase.job :refer [get-job set-job]]
            [puppeteer.domain.usecase.deploy :as deploy-usecase]))

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
   [_ _ user-name repo-name branch-name subdomain]]
  (try
    (as-> (map->Job {:user-name user-name
                     :repo-name repo-name
                     :branch-name branch-name
                     :subdomain subdomain
                     :message m}) $
      (assoc $ :conf (load-conf conf-usecase $))
      (assoc-in $ [:build :id] (build build-usecase $))
      (do (message-usecase/send-deploy-message message-usecase $) $)
      (set-job job-usecase $))
    (catch Exception e
      (do
        (message-usecase/send-build-failure-message message-usecase
                                                    {:message m
                                                     :user-name user-name
                                                     :repo-name repo-name
                                                     :branch-name branch-name
                                                     :error-message (.getMessage e)})
        (throw e)))))

(defn- roundup
  [{:keys [message-usecase deploy-usecase]}
   m
   [_ _ user-name repo-name branch-name subdomain]]
  (try
    (as-> (map->Job {:user-name user-name
                    :repo-name repo-name
                    :branch-name branch-name
                    :subdomain subdomain
                    :message m}) $
      (do (deploy-usecase/round-up deploy-usecase $) $)
      (do (message-usecase/send-round-up-succeed-message message-usecase $) $))
    (catch Exception e
      (do
        (message-usecase/send-round-up-failure-message
          message-usecase
          {:message m
           :user-name user-name
           :repo-name repo-name
           :branch-name branch-name
           :error-message (.getMessage e)})
        (throw e)))))

(defn- build-succeed
  [{:keys [message-usecase build-usecase conf-usecase job-usecase deploy-usecase]}
   m]
  (let [job (get-job job-usecase (:id m))]
    (when job
      (try
        (as-> job $
          (assoc $ :build m)
          (do (message-usecase/send-build-succeed-message message-usecase $) $)
          (do (message-usecase/send-deploy-start-message message-usecase $) $)
          (do (deploy-usecase/apply deploy-usecase $) $)
          (do (message-usecase/send-deploy-succeed-message message-usecase $) $))
        (catch Exception e
          (do
            (message-usecase/send-deploy-failure-message
              message-usecase
              (-> job
                  (assoc :error-message (.getMessage e))))
            (throw e)))))))

(defn- build-failure
  [{:keys [message-usecase build-usecase conf-usecase job-usecase]}
   m]
  (let [job (get-job job-usecase (:id m))]
    (when job
      (message-usecase/send-build-failure-message
        message-usecase
        {:message (:message job)
         :user-name (:user-name job)
         :repo-name (:repo-name job)
         :branch-name (:branch-name job)
         :error-message (:logUrl m)}))))

(defmulti reaction
  (fn [_ m] (:type m)))

(defmethod reaction :message
  [{:keys [message-usecase build-usecase] :as comp}
   m]
  (let [args (some-> m :text (clojure.string/split #" "))
        [_ command _] args]
    (if (and (:for-me? m) (not (:from-me? m)))
      (case command
        "deploy" (async/go (deploy comp m args))
        "roundup" (async/go (roundup comp m args))
        (help comp m args)))))

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
