(ns tubo.api.streams
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tubo.api.items :as items])
  (:import
   org.schabi.newpipe.extractor.stream.StreamInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.DateWrapper
   java.time.Instant))

(defn get-stream
  [url]
  (let [info (StreamInfo/getInfo (url-decode url))]
    {:name (.getName info)
     :url (.getUrl info)
     :description (.. info (getDescription) (getContent))
     :upload-date (.getTextualUploadDate info)
     :uploader-name (.getUploaderName info)
     :uploader-url (.getUploaderUrl info)
     :uploader-avatar (.getUploaderAvatarUrl info)
     :uploader-verified? (.isUploaderVerified info)
     :service-id (.getServiceId info)
     :thumbnail-url (.getThumbnailUrl info)
     :duration (.getDuration info)
     :tags (.getTags info)
     :category (.getCategory info)
     :view-count (when-not (= (.getViewCount info) -1) (.getViewCount info))
     :like-count (when-not (= (.getLikeCount info) -1) (.getLikeCount info))
     :dislike-count (when-not (= (.getDislikeCount info) -1) (.getDislikeCount info))
     :subscriber-count (when-not (= (.getUploaderSubscriberCount info) -1) (.getUploaderSubscriberCount info))
     :audio-streams (j/from-java (.getAudioStreams info))
     :video-streams (j/from-java (.getVideoStreams info))
     :hls-url (.getHlsUrl info)
     :dash-mpd-url (.getDashMpdUrl info)
     :related-streams (items/get-items (.getRelatedStreams info))}))
