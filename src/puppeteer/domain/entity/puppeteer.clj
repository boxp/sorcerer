(ns puppeteer.domain.entity.puppeteer
  (:require [puppeteer.domain.entity.build :refer [map->BuildStep]]))

(defrecord Puppeteer [steps images pod])
