(ns tau.api.result)

(defrecord PlaylistResult
    [name thumbnail-url url upload-author stream-count])

(defrecord ChannelResult
    [name description verified? thumbnail-url url
     subscriber-count stream-count])

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
                false)
    :verified? (.isUploaderVerified stream)}))

(defn get-channel-result
  [channel]
  (map->ChannelResult
   {:url (.getUrl channel)
    :name (.getName channel)
    :thumbnail-url (.getThumbnailUrl channel)
    :description (.getDescription channel)
    :subscriber-count (.getSubscriberCount channel)
    :stream-count (.getStreamCount channel)
    :verified? (.isVerified channel)}))

(defn get-playlist-result
  [playlist]
  (map->PlaylistResult
   {:url (.getUrl playlist)
    :name (.getName playlist)
    :thumbnail-url (.getThumbnailUrl playlist)
    :upload-author (.getUploaderName playlist)
    :stream-count (.getStreamCount playlist)}))

(defn get-results
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM" (get-stream-result %)
          "CHANNEL" (get-channel-result %)
          "PLAYLIST" (get-playlist-result %))
       items))
