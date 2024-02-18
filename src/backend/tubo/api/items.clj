(ns tubo.api.items)

(defn get-stream-item
  [stream]
  {:type :stream
   :service-id (.getServiceId stream)
   :url (.getUrl stream)
   :name (.getName stream)
   :thumbnail-url (.getThumbnailUrl stream)
   :uploader-name (.getUploaderName stream)
   :uploader-url (.getUploaderUrl stream)
   :uploader-avatar (.getUploaderAvatarUrl stream)
   :upload-date (.getTextualUploadDate stream)
   :short-description (.getShortDescription stream)
   :duration (.getDuration stream)
   :view-count (when-not (= (.getViewCount stream) -1) (.getViewCount stream))
   :uploaded (when (.getUploadDate stream)
               (.. stream (getUploadDate) (offsetDateTime) (toInstant) (toEpochMilli)))
   :verified? (.isUploaderVerified stream)})

(defn get-channel-item
  [channel]
  {:type :channel
   :service-id (.getServiceId channel)
   :url (.getUrl channel)
   :name (.getName channel)
   :thumbnail-url (.getThumbnailUrl channel)
   :description (.getDescription channel)
   :subscriber-count (when-not (= (.getSubscriberCount channel) -1) (.getSubscriberCount channel))
   :stream-count (when-not (= (.getStreamCount channel) -1) (.getStreamCount channel))
   :verified? (.isVerified channel)})

(defn get-playlist-item
  [playlist]
  {:type :playlist
   :service-id (.getServiceId playlist)
   :url (.getUrl playlist)
   :name (.getName playlist)
   :thumbnail-url (.getThumbnailUrl playlist)
   :uploader-name (.getUploaderName playlist)
   :stream-count (when-not (= (.getStreamCount playlist) -1) (.getStreamCount playlist))})

(defn get-items
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM" (get-stream-item %)
          "CHANNEL" (get-channel-item %)
          "PLAYLIST" (get-playlist-item %))
       items))
