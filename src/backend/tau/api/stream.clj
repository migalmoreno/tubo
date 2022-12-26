(ns tau.api.stream
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tau.api.result :as result])
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

(defn get-info
  [url]
  (let [info (StreamInfo/getInfo (url-decode url))]
    (map->Stream
     {:name (.getName info)
      :url (.getUrl info)
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
      :like-count (if (= (.getLikeCount info) -1) nil (.getLikeCount info))
      :dislike-count (if (= (.getDislikeCount info) -1) nil (.getDislikeCount info))
      :subscriber-count (if (= (.getUploaderSubscriberCount info) -1) nil (.getUploaderSubscriberCount info))
      :audio-streams (j/from-java (.getAudioStreams info))
      :video-streams (j/from-java (.getVideoStreams info))
      :hls-url (.getHlsUrl info)
      :dash-mpd-url (.getDashMpdUrl info)
      :related-streams (result/get-results (.getRelatedStreams info))})))
