(ns tau.api.stream
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.stream.StreamInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.DateWrapper
   java.time.Instant))

(defrecord Stream
    [name description upload-date
     upload-author upload-url upload-avatar
     thumbnail-url service-id duration view-count like-count
     dislike-count subscriber-count upload-verified? hls-url
     dash-mpd-url category tags audio-streams video-streams
     related-streams])

(defrecord StreamResult
    [name url thumbnail-url upload-author upload-url
     upload-avatar upload-date short-description
     duration view-count uploaded verified?])

(defn get-stream-result
  [stream]
  (map->StreamResult
   {:url (.getUrl stream)
    :name (.getName stream)
    :thumbnail-url (.getThumbnailUrl stream)
    :upload-author (.getUploaderName stream)
    :upload-url (.getUploaderUrl stream)
    :upload-avatar (.getUploaderAvatarUrl stream)
    :upload-date (.getTextualUploadDate stream)
    :short-description (.getShortDescription stream)
    :duration (.getDuration stream)
    :view-count (.getViewCount stream)
    :uploaded (if (.getUploadDate stream)
                (.. stream (getUploadDate) (offsetDateTime) (toInstant) (toEpochMilli))
                -1)
    :verified? (.isUploaderVerified stream)}))

(defn get-stream-info
  [url]
  (let [info (StreamInfo/getInfo (url-decode url))]
    (map->Stream
     {:name (.getName info)
      :description (.. info (getDescription) (getContent))
      :upload-date (.getTextualUploadDate info)
      :upload-author (.getUploaderName info)
      :upload-url (.getUploaderUrl info)
      :upload-avatar (.getUploaderAvatarUrl info)
      :upload-verified? (.isUploaderVerified info)
      :service-id (.getServiceId info)
      :thumbnail-url (.getThumbnailUrl info)
      :duration (.getDuration info)
      :tags (.getTags info)
      :category (.getCategory info)
      :view-count (.getViewCount info)
      :like-count (.getLikeCount info)
      :dislike-count (.getDislikeCount info)
      :subscriber-count (.getUploaderSubscriberCount info)
      :audio-streams (j/from-java (.getAudioStreams info))
      :video-streams (j/from-java (.getVideoStreams info))
      :hls-url (.getHlsUrl info)
      :dash-mpd-url (.getDashMpdUrl info)
      :related-streams (map #(get-stream-result %) (.getRelatedStreams info))})))
