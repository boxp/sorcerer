(ns puppeteer.domain.entity.build
  (:require [clojure.spec.alpha :as s]))

(s/def ::project-id string?)
(s/def ::user-name string?)
(s/def ::repo-name string?)
(s/def ::branch-name string?)
(s/def ::tag-name string?)
(s/def ::commit-sha string?)

(s/def ::repo-source
  (s/keys :req-un [::project-id ::repo-name ::branch-name ::tag-name ::commit-sha]))
(s/def ::source
  (s/keys :req-un [::repo-source]))

(s/def :volume/name string?)
(s/def :volume/path string?)
(s/def ::volume
  (s/keys :req-un [:volume/name :volume/path]))

(s/def :build-step/name string?)
(s/def :build-step/args (s/coll-of string?))
(s/def :build-step/entrypoint string?)
(s/def :build-step/volumes (s/coll-of ::volume))
(s/def :build-step/secretEnv (s/coll-of string?))
(s/def ::build-step
  (s/keys :req-un [:build-step/name :build-step/args]
          :opt-un [:build-step/entrypoint
                   :build-step/volumes
                   :build-step/secretEnv]))

(s/def :build/steps (s/coll-of ::build-step))
(s/def :build/images (s/coll-of string?))
(s/def ::build
  (s/keys :req-un [::source :build/steps :build/images]))

;; TODO: ::build-message
(s/def ::build-message
  (s/keys :req-un []))

(defrecord BuildMessage [id projectId status source steps createTime startTime finishTime timeout images logsBucket sourceProvenance logUrl])
