(ns sorcerer.domain.entity.job
  (:require [clojure.spec.alpha :as s]
            [sorcerer.domain.entity.conf :as conf]
            [sorcerer.domain.entity.build :as build]
            [sorcerer.domain.entity.message :as message]))

(s/def :job/user-name ::build/user-name)
(s/def :job/conf ::conf/configuration)
(s/def :job/build ::build/build)
(s/def :job/message ::message/message)
(s/def :job/subdomain (s/nilable string?))
(s/def ::job
  (s/keys :req-un [:job/conf
                   :job/build
                   :job/message
                   :job/user-name
                   ::build/repo-name
                   ::build/branch-name
                   :job/subdomain]))

(defrecord Job [conf build message user-name repo-name branch-name subdomain])
