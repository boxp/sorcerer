(ns puppeteer.domain.entity.message
  (:require [clojure.spec.alpha :as s]))

(s/def :field/title string?)
(s/def :field/value string?)
(s/def :field/short? boolean?)
(s/def ::field
  (s/keys :req-un [:field/title :field/value :field/short?]))

(s/def :attachment/text string?)
(s/def :attachment/color string?)
(s/def :attachment/fields (s/coll-of ::field))
(s/def ::attachment
  (s/keys :req-un [:attachment/text]
          :opt-un [:attachment/color :attachment/fields]))

(s/def :message/channel-id string?)
(s/def :message/user-id string?)
(s/def :message/text string?)
(s/def :message/timestamp string?)
(s/def :message/attachments (s/coll-of ::attachment))
(s/def :message/for-me? boolean?)
(s/def :message/from-me? boolean?)
(s/def ::message
  (s/keys :req-un [:message/channel-id
                   :message/user-id
                   :message/text]
          :opt-un [:message/timestamp
                   :message/attachments
                   :message/for-me?
                   :message/from-me?]))

(defrecord Field
  [title value short?])

(defrecord Attachment
  [text color fields])

(defrecord Message
  [channel-id user-id text timestamp attachments for-me? from-me?])
