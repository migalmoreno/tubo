(ns tubo.handlers.stream
  (:require
   [clojure.java.data :as j]
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.stream.StreamInfo))

(defn get-stream
  [{{:keys [url]} :path-params :as req}]
  (let [info (StreamInfo/getInfo (url-decode url))]
    {:name               (.getName info)
     :service-id         (.getServiceId info)
     :related-streams    (utils/get-items (.getRelatedItems info) req)
     :url                (.getUrl info)
     :thumbnails         (utils/proxy-images (.getThumbnails info) req)
     :description        (.. info (getDescription) (getContent))
     :duration           (.getDuration info)
     :upload-date        (.getTextualUploadDate info)
     :uploader-name      (.getUploaderName info)
     :uploader-url       (.getUploaderUrl info)
     :uploader-avatars   (utils/proxy-images (.getUploaderAvatars info) req)
     :uploader-verified? (.isUploaderVerified info)
     :tags               (.getTags info)
     :category           (.getCategory info)
     :view-count         (utils/non-negative (.getViewCount info))
     :like-count         (utils/non-negative (.getLikeCount info))
     :dislike-count      (utils/non-negative (.getDislikeCount info))
     :subscriber-count   (utils/non-negative (.getUploaderSubscriberCount info))
     :audio-streams      (j/from-java-deep (.getAudioStreams info)
                                           {:exceptions :omit})
     :video-streams      (j/from-java-deep (.getVideoStreams info)
                                           {:exceptions :omit})
     :video-only-streams (j/from-java-deep (.getVideoOnlyStreams info)
                                           {:exceptions :omit})
     :hls-url            (.getHlsUrl info)
     :dash-mpd-url       (.getDashMpdUrl info)
     :preview-frames     (j/from-java (.getPreviewFrames info))
     :stream-segments    (j/from-java (.getStreamSegments info))
     :support-info       (.getSupportInfo info)
     :short?             (.isShortFormContent info)
     :license            (.getLicence info)
     :privacy            (.getPrivacy info)
     :subtitles          (j/from-java-deep (.getSubtitles info)
                                           {:exceptions :omit})}))

(defn create-stream-handler
  [req]
  (ok (get-stream req)))
