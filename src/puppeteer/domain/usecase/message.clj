(ns puppeteer.domain.usecase.message
  (:require [com.stuartsierra.component :as component]
            [puppeteer.util :refer [->host]]
            [puppeteer.domain.entity.message :refer [map->Message map->Attachment map->Field]]
            [puppeteer.infra.repository.message :as r]))

(defn send-help-message
  [{:keys [message-repository]}
   {:keys [message]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ":dolls:"
        :attachments [(map->Attachment
                        {:text (str ":rocket: Deploy: @alc deploy <user-name> <repository-name> <branch-name>" "\n"
                                    ":wastebasket: RoundUp: @alc roundup <user-name> <repository-name> <branch-name>")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":hammer_and_wrench: Building " user-name "/" repo-name "/" branch-name " ...")})]}
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
   {:keys [message user-name repo-name branch-name error-message]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:pretext (str ":innocent: Building Failure... " user-name "/" repo-name "/" branch-name)
                         :text error-message
                         :color "danger"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-start-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":hammer_and_wrench: Deploying " user-name "/" repo-name "/" branch-name " ...")})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-succeed-message
  [{:keys [message-repository domain]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":tada: Deploy Completed! "
                                    "https://"
                                    (->host {:domain domain
                                             :repo-name repo-name
                                             :branch-name branch-name}))
			 :color "good"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-deploy-failure-message
  [{:keys [message-repository domain]}
   {:keys [message user-name repo-name branch-name error-message]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:pretext (str ":innocent: Deploy Failure... " user-name "/" repo-name "/" branch-name)
                         :text (str error-message)
			 :color "danger"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-round-up-succeed-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:text (str ":wave: RoundUp Completed! " user-name "/" repo-name "/" branch-name)
			 :color "good"})]}
       map->Message
       (r/send-message message-repository)))

(defn send-round-up-failure-message
  [{:keys [message-repository]}
   {:keys [message user-name repo-name branch-name error-message]}]
  (->> {:channel-id (:channel-id message)
        :user-id (:user-id message)
        :text ""
        :attachments [(map->Attachment
                        {:pretext (str ":innocent: RoundUp Failure... " user-name "/" repo-name "/" branch-name)
                         :text error-message
			 :color "danger"})]}
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
