(ns puppeteer.domain.usecase.message
  (:require [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.infra.repository.message :as r]))

(defn send-help-message
  [{:keys [message-repository]}
   {:keys [received-message]}]
  (->> {:channel-id (:channel-id received-message)
        :user-id (:user-id received-message)
        :text ":dolls:"
        :attachments [(map->Attachment
                        {:text (str ":rocket: Deploy: @alc deploy <user-name> <repository-name> <branch-name>")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-message
  [{:keys [message-repository]}
   {:keys [received-message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id received-message)
        :user-id (:user-id received-message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":loading: Building " user-name "/" repo-name "/" branch-name " ...")})
                      (map->Attachment
                        {:text (str ":clock9: Deploy " user-name "/" repo-name "/" branch-name " ...")})]}
       map->Message
       (r/send-message message-repository)))

(defn update-message
  [{:keys [message-repository]}
   {:keys [message]}]
  (r/update-message message-repository message))

(defn subscribe-message
  [{:keys [message-repository]}]
  (r/subscribe-message message-repository))

(defrecord MessageUsecaseComponent [message-repository]
  component/Lifecycle
  (start [{:keys [message-repository] :as this}]
    (println ";; Starting MessageUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping MessageUsecaseComponent")
    this))

(defn message-usecase-component
  []
  (map->MessageUsecaseComponent {}))
