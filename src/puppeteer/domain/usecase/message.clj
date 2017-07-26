(ns puppeteer.domain.usecase.message
  (:require [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.infra.repository.message :as r]))

(defn send-build-message
  [{:keys [message-repository]}
   {:keys [received-message repo-name branch-name]}]
  (->> {:channel-id (:channel-id received-message)
        :user-id (:user-id received-message)
        :text ""
        :attachments [(map->Attachment
                        {:fields
                         (map->Field
                           {:title (str ":loading: Building " repo-name "/" branch-name " ...")})})]}
       map->Message
       (r/send-message message-repository)))

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
