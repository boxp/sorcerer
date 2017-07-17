(ns puppeteer.domain.entity.build)

(defrecord RepoSource [project-id repo-name branch-name tag-name commit-sha])

(defrecord Source [repo-source])

(defrecord BuildStep [name args])

(defrecord Build [source steps images])

(defrecord BuildMessage [id project-id status source steps create-time start-time finish-time timeout images logs-bucket source-provenance log-url])
