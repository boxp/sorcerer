(ns puppeteer.domain.entity.message)

(defrecord Field
  [title value short?])

(defrecord Attachment
  [text color fields])

(defrecord Message
  [channel-id user-id text timestamp attachments for-me? from-me?])
