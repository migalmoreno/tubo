(ns tubo.handlers.stream
  (:require
   [ring.util.codec :refer [url-decode]]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.stream.StreamInfo))

(defn ->Streams
  [items items*]
  (map-indexed (fn [idx item]
                 (assoc item
                        :mime-type
                        (.. (.get items* idx)
                            getFormat
                            getMimeType)))
               items))

(defn create-stream-handler
  [{{:keys [url]} :path-params :as req}]
  (when-let [info (StreamInfo/getInfo (url-decode url))]
    (ok
     (-> (utils/->ListInfo info req)
         (update :duration #(when (pos-int? %) %))
         (update :view-count #(when (pos-int? %) %))
         (update :like-count #(when (pos-int? %) %))
         (update :dislike-count #(when (pos-int? %) %))
         (update :uploader-subscriber-count #(when (pos-int? %) %))
         (update :thumbnails #(utils/proxy-images % req))
         (update :uploader-avatars #(utils/proxy-images % req))
         (update :audio-streams #(->Streams % (.getAudioStreams info)))
         (update :video-streams #(->Streams % (.getVideoStreams info)))
         (update :video-only-streams
                 #(->Streams % (.getVideoOnlyStreams info)))))))
