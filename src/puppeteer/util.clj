(ns puppeteer.util
  (:require [clojure.string :as string]))

(defn- sanitize-name
  [name]
  (string/replace name #"[\\\/\_\.]" "-"))

(defn ->app
  [{:keys [repo-name branch-name] :as job}]
  (str (sanitize-name repo-name) "-" (sanitize-name branch-name)))

(defn ->host
  [{:keys [domain repo-name branch-name]}]
  (str (sanitize-name repo-name) "-" (sanitize-name branch-name) "." domain))

(defn subdomain->host
  [domain subdomain]
  (str subdomain "." domain))

(defn ->service-name
  [{:keys [repo-name branch-name] :as job}]
  (str (sanitize-name repo-name) "-" (sanitize-name branch-name)))

