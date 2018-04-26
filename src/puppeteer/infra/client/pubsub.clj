(ns puppeteer.infra.client.pubsub
  (:import (com.google.protobuf ByteString)
           (com.google.common.util.concurrent MoreExecutors)
           (com.google.api.core ApiFutures
                                ApiFutureCallback
                                ApiService$Listener)
           (com.google.cloud ServiceOptions)
           (com.google.cloud.pubsub.v1 TopicAdminClient
                                           Publisher
                                           SubscriptionAdminClient
                                           Subscriber
                                           MessageReceiver)
           (com.google.pubsub.v1 Topic
                                 TopicName
                                 SubscriptionName
                                 PubsubMessage
                                 PushConfig))
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(s/def ::project-id string?)
(s/def ::topic-key keyword?)
(s/def ::subscription-key keyword?)
(s/def ::subscribers (s/nilable (s/map-of keyword? #(instance? Subscriber %))))
(s/def ::pubsub-subscription-component
  (s/keys :opt-un [::project-id
                   ::subscribers]))

(s/fdef create-topic
  :args (s/cat :comp ::pubsub-subscription-component
               :topic-key ::topic-key)
  :ret #(instance? TopicName %))
(defn create-topic
  [comp topic-key]
  (let [topic-admin-cli (TopicAdminClient/create)]
    (try
        (->> (TopicName/create (:project-id comp)
                               (name topic-key))
             (.createTopic topic-admin-cli))
        (catch Exception e
          (TopicName/create (:project-id comp)
            (name topic-key))))))

(s/fdef create-subscription
  :args (s/cat :comp ::pubsub-subscription-component
               :topic-key ::topic-key
               :subscription-key ::subscription-key)
  :ret true?)
(defn create-subscription
  [comp topic-key subscription-key]
  (let [topic-name (create-topic comp topic-key)
        subscription-name (SubscriptionName/create (:project-id comp) (name subscription-key))
        push-config (-> (PushConfig/newBuilder) .build)
        ack-deadline-second 0]
    (-> (SubscriptionAdminClient/create)
        (.createSubscription subscription-name
                             topic-name
                             push-config
                             ack-deadline-second))))

(s/def :add-subscriber-args/on-receive
  (s/fspec
    :args (s/cat :message #(instance? PubsubMessage %))
    :ret nil?))
(s/fdef add-subscriber
  :args (s/cat :comp ::pubsub-subscription-component
               :topic-key ::topic-key
               :subscription-key ::subscription-key
               :on-receive :add-subscriber-args/on-receive)
  :ret ::pubsub-subscription-component)
(defn add-subscriber
  [comp topic-key subscription-key on-receive]
  (let [subscription-name (SubscriptionName/create (:project-id comp) (name subscription-key))
        receiver (reify MessageReceiver
                   (receiveMessage [this message consumer]
                     (on-receive message)
                     (.ack consumer)))
        listener (proxy [ApiService$Listener] []
                   (failed [from failure]))
        subscriber (-> (Subscriber/defaultBuilder subscription-name receiver) .build)]
    (.addListener subscriber listener (MoreExecutors/directExecutor))
    (-> subscriber .startAsync .awaitRunning)
    (-> comp
        (assoc-in [:subscribers subscription-key] subscriber))))

(defrecord PubSubSubscriptionComponent [project-id subscribers]
  component/Lifecycle
  (start [this]
    (println ";; Starting PubSubSubscriptionComponent")
    (-> this
        (assoc :project-id (ServiceOptions/getDefaultProjectId))))
  (stop [this]
    (println ";; Stopping PubSubSubscriptionComponent")
    (doall (map #(.stopAsync %) (:subscribers this)))
    (-> this
        (dissoc :project-id)
        (dissoc :subscribers))))

(s/fdef pubsub-subscription-component
  :args (s/cat)
  :ret ::pubsub-subscription-component)
(defn pubsub-subscription-component
  []
  (map->PubSubSubscriptionComponent {}))
