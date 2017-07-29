(ns puppeteer.domain.entity.conf)

(defrecord K8s
  [deployment service])

(defrecord Configuration
  [steps images k8s])
