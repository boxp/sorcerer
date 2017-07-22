(defproject puppeteer "0.1.0-SNAPSHOT"
  :description "A Slackbot for Deploying microservices to GKE Cluster"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [slack-rtm "0.1.5"]
                 [ring "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.0"]
                 [cheshire "5.7.1"]
                 [circleci/clj-yaml "0.5.5"]
                 [clj-http "3.6.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [com.taoensso/faraday "1.8.0"]
                 [io.fabric8/kubernetes-client "2.5.6"]
                 [io.fabric8/kubernetes-model "1.1.0"]
                 [com.google.guava/guava "22.0"]
                 [com.google.auth/google-auth-library-oauth2-http "0.6.1"]
                 [com.google.auth/google-auth-library-credentials "0.6.1"]
                 [com.google.cloud/google-cloud-pubsub "0.20.1-beta"
                  :exclusions [com.google.auth/google-auth-library-oauth2-http
                               com.google.auth/google-auth-library-credentials
                               com.google.guava/guava]]
                 [com.google.apis/google-api-services-cloudbuild "v1-rev597-1.22.0"
                  :exclusions [com.google.guava/guava
                               com.google.guava/guava-jdk5]]]
  :profiles
  {:dev {:source-paths ["src" "dev"]}
   :uberjar {:main puppeteer.system}})
