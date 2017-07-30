(ns puppeteer.domain.entity.build)

(defrecord RepoSource [project-id repo-name branch-name tag-name commit-sha])

(defrecord Source [repo-source])

(defrecord BuildStep [name args])

(defrecord Build [source steps images])

(defrecord BuildMessage [id projectId status source steps createTime startTime finishTime timeout images logsBucket sourceProvenance logUrl])
