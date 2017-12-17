(ns puppeteer.domain.entity.build)

(defrecord RepoSource [project-id repo-name branch-name tag-name commit-sha])

(defrecord Source [repo-source])

(defrecord Volume [name path])

(defrecord BuildStep [name args entrypoint volumes])

(defrecord Build [source steps images])

(defrecord BuildMessage [id projectId status source steps createTime startTime finishTime timeout images logsBucket sourceProvenance logUrl])
