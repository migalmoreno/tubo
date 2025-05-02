(ns tubo.handlers.utils
  (:require
   [clojure.java.data :as j]))

(defn non-negative
  [val]
  (when-not (= val -1) val))

(defn get-stream-item
  [stream]
  {:type               :stream
   :service-id         (.getServiceId stream)
   :url                (.getUrl stream)
   :name               (.getName stream)
   :thumbnails         (j/from-java (.getThumbnails stream))
   :short?             (.isShortFormContent stream)
   :uploader-name      (.getUploaderName stream)
   :uploader-url       (.getUploaderUrl stream)
   :uploader-avatars   (j/from-java (.getUploaderAvatars stream))
   :upload-date        (.getTextualUploadDate stream)
   :short-description  (.getShortDescription stream)
   :duration           (non-negative (.getDuration stream))
   :view-count         (non-negative (.getViewCount stream))
   :stream-type        (.getStreamType stream)
   :uploaded           (when (.getUploadDate stream)
                         (.. stream
                             (getUploadDate)
                             (offsetDateTime)
                             (toInstant)
                             (toEpochMilli)))
   :uploader-verified? (.isUploaderVerified stream)})

(defn get-channel-item
  [channel]
  {:type             :channel
   :service-id       (.getServiceId channel)
   :url              (.getUrl channel)
   :name             (.getName channel)
   :thumbnails       (j/from-java (.getThumbnails channel))
   :description      (.getDescription channel)
   :subscriber-count (non-negative (.getSubscriberCount channel))
   :stream-count     (non-negative (.getStreamCount channel))
   :verified?        (.isVerified channel)})

(defn get-playlist-item
  [playlist]
  {:type          :playlist
   :service-id    (.getServiceId playlist)
   :url           (.getUrl playlist)
   :name          (.getName playlist)
   :thumbnails    (j/from-java (.getThumbnails playlist))
   :uploader-name (.getUploaderName playlist)
   :uploader-url  (.getUploaderUrl playlist)
   :description   (.getDescription playlist)
   :playlist-type (.getPlaylistType playlist)
   :stream-count  (non-negative (.getStreamCount playlist))})

(defn get-items
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM"   (get-stream-item %)
          "CHANNEL"  (get-channel-item %)
          "PLAYLIST" (get-playlist-item %))
       items))
