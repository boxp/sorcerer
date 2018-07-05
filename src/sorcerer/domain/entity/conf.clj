(ns sorcerer.domain.entity.conf
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [ends-with?]]
            [sorcerer.domain.entity.build :as build]))

(s/def ::k8s-resource (s/and string?
                           (s/or :yml #(ends-with? % ".yml")
                                 :yaml #(ends-with? % ".yaml"))))

(s/def :k8s/deployment ::k8s-resource)
(s/def :k8s/service ::k8s-resource)
(s/def ::k8s
  (s/keys :req-un [:k8s/deployment :k8s/service]))

(s/def :secret/kmsKeyName string?)
(s/def :secret/secretEnv (s/map-of string? string?))
(s/def ::secret
  (s/keys :req-un [:secret/kmsKeyName :secret/secretEnv]))

(s/def :configuration/steps (s/coll-of ::build/build-step))
(s/def :configuration/images (s/coll-of string?))
(s/def :configuration/timeout string?)
(s/def :configuration/secrets (s/coll-of ::secret))
(s/def ::configuration
  (s/keys :req-un [:configuration/steps
                   :configuration/images
                   ::k8s
                   :configuration/timeout
                   :configuration/secrets]))

(defrecord K8s
  [deployment service])

(defrecord Configuration
  [steps images k8s timeout secrets])
