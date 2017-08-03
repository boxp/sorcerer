(ns puppeteer.domain.usecase.message
  (:require [com.stuartsierra.component :as component]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.infra.repository.message :as r]))

(defn send-help-message
  [{:keys [message-repository]}
   {:keys [message]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ":dolls:"
        :attachments [(map->Attachment
                        {:text (str ":rocket: Deploy: @alc deploy <user-name> <repository-name> <branch-name>")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":loading: Building " user-name "/" repo-name "/" branch-name " ...")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-build-succeed-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":tada: Building Completed! " user-name "/" repo-name "/" branch-name)
                         :color "good"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-build-failure-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":innocent: Building Failure... " user-name "/" repo-name "/" branch-name)
                         :color "danger"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-start-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":loading: Deploying " user-name "/" repo-name "/" branch-name " ...")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-succeed-message
  [{:keys [message-repository domain]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":tada: Deploy Completed! " "https://" repo-name "-" branch-name "." domain)
			 :color "good"})]}
       map->Message
       (r/send-message message-repository)))

(defn update-message
  [{:keys [message-repository]}
   {:keys [message]}]
  (r/update-message message-repository message))

(defn subscribe-message
  [{:keys [message-repository]}]
  (r/subscribe-message message-repository))

(defrecord MessageUsecaseComponent [message-repository domain]
  component/Lifecycle
  (start [{:keys [message-repository] :as this}]
    (println ";; Starting MessageUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping MessageUsecaseComponent")
    this))

(defn message-usecase-component
  [domain]
  (map->MessageUsecaseComponent {:domain domain}))
