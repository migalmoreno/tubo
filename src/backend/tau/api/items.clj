(ns tau.api.items)

(defn get-stream-item
  [stream]
  {:type :stream
   :url (.getUrl stream)
   :name (.getName stream)
   :thumbnail-url (.getThumbnailUrl stream)
   :upload-author (.getUploaderName stream)
   :upload-url (.getUploaderUrl stream)
   :upload-avatar (.getUploaderAvatarUrl stream)
   :upload-date (.getTextualUploadDate stream)
   :short-description (.getShortDescription stream)
   :duration (.getDuration stream)
   :view-count (when-not (= (.getViewCount stream) -1) (.getViewCount stream))
   :uploaded (if (.getUploadDate stream)
               (.. stream (getUploadDate) (offsetDateTime) (toInstant) (toEpochMilli))
               false)
   :verified? (.isUploaderVerified stream)})

(defn get-channel-item
  [channel]
  {:type :channel
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
   :url (.getUrl playlist)
   :name (.getName playlist)
   :thumbnail-url (.getThumbnailUrl playlist)
   :upload-author (.getUploaderName playlist)
   :stream-count (when-not (= (.getStreamCount playlist) -1) (.getStreamCount playlist))})

(defn get-items
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM" (get-stream-item %)
          "CHANNEL" (get-channel-item %)
          "PLAYLIST" (get-playlist-item %))
       items))
