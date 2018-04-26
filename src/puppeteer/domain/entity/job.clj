(ns puppeteer.domain.entity.job
  (:require [clojure.spec.alpha :as s]
            [puppeteer.domain.entity.conf :as conf]
            [puppeteer.domain.entity.build :as build]
            [puppeteer.domain.entity.message :as message]))

(s/def :job/user-name ::build/user-name)
(s/def :job/conf ::conf/configuration)
(s/def :job/build ::build/build)
(s/def :job/message ::message/message)
(s/def :job/reserved-subdomain (s/nilable string?))
(s/def ::job
  (s/keys :req-un [:job/conf
                   :job/build
                   :job/message
                   :job/user-name
                   ::build/repo-name
                   ::build/branch-name
                   :job/reserved-subdomain]))

(defrecord Job [conf build message user-name repo-name branch-name reserved-subdomain])
