(ns tubo.api
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.search.SearchInfo
   org.schabi.newpipe.extractor.stream.StreamInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

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
   :uploaded
   (when (.getUploadDate stream)
     (.. stream (getUploadDate) (offsetDateTime) (toInstant) (toEpochMilli)))
   :verified? (.isUploaderVerified stream)})

(defn get-channel-item
  [channel]
  {:type             :channel
   :service-id       (.getServiceId channel)
   :url              (.getUrl channel)
   :name             (.getName channel)
   :thumbnail-url    (.getThumbnailUrl channel)
   :description      (.getDescription channel)
   :subscriber-count (when-not (= (.getSubscriberCount channel) -1)
                       (.getSubscriberCount channel))
   :stream-count     (when-not (= (.getStreamCount channel) -1)
                       (.getStreamCount channel))
   :verified?        (.isVerified channel)})

(defn get-playlist-item
  [playlist]
  {:type          :playlist
   :service-id    (.getServiceId playlist)
   :url           (.getUrl playlist)
   :name          (.getName playlist)
   :thumbnail-url (.getThumbnailUrl playlist)
   :uploader-name (.getUploaderName playlist)
   :stream-count  (when-not (= (.getStreamCount playlist) -1)
                    (.getStreamCount playlist))})

(defn get-items
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM"   (get-stream-item %)
          "CHANNEL"  (get-channel-item %)
          "PLAYLIST" (get-playlist-item %))
       items))

(defn get-common-info
  [info]
  {:name            (.getName info)
   :service-id      (.getServiceId info)
   :related-streams (get-items (.getRelatedItems info))})

(defn get-channel
  ([url]
   (let [info (ChannelInfo/getInfo (url-decode url))]
     (merge (get-common-info info)
            {:id               (.getId info)
             :verified?        (.isVerified info)
             :banner           (.getBannerUrl info)
             :description      (.getDescription info)
             :avatar           (.getAvatarUrl info)
             :subscriber-count (when-not (= (.getSubscriberCount info) -1)
                                 (.getSubscriberCount info))
             :donation-links   (.getDonationLinks info)
             :next-page        (j/from-java (.getNextPage info))})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info    (ChannelInfo/getMoreItems service
                                           (url-decode url)
                                           (Page. (url-decode page-url)))]
     {:related-streams (get-items (.getItems info))
      :next-page       (j/from-java (.getNextPage info))})))

(defn get-stream
  [url]
  (let [info (StreamInfo/getInfo (url-decode url))]
    (merge (get-common-info info)
           {:url                (.getUrl info)
            :thumbnail-url      (.getThumbnailUrl info)
            :description        (.. info (getDescription) (getContent))
            :duration           (.getDuration info)
            :upload-date        (.getTextualUploadDate info)
            :uploader-name      (.getUploaderName info)
            :uploader-url       (.getUploaderUrl info)
            :uploader-avatar    (.getUploaderAvatarUrl info)
            :uploader-verified? (.isUploaderVerified info)
            :tags               (.getTags info)
            :category           (.getCategory info)
            :view-count         (when-not (= (.getViewCount info) -1)
                                  (.getViewCount info))
            :like-count         (when-not (= (.getLikeCount info) -1)
                                  (.getLikeCount info))
            :dislike-count      (when-not (= (.getDislikeCount info) -1)
                                  (.getDislikeCount info))
            :subscriber-count   (when-not (= (.getUploaderSubscriberCount info)
                                             -1)
                                  (.getUploaderSubscriberCount info))
            :audio-streams      (j/from-java (.getAudioStreams info))
            :video-streams      (j/from-java (.getVideoStreams info))
            :hls-url            (.getHlsUrl info)
            :dash-mpd-url       (.getDashMpdUrl info)})))

(defn get-playlist
  ([url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info    (PlaylistInfo/getInfo service (url-decode url))]
     (merge (get-common-info info)
            {:id              (.getId info)
             :playlist-type   (j/from-java (.getPlaylistType info))
             :thumbnail-url   (.getThumbnailUrl info)
             :banner-url      (.getBannerUrl info)
             :uploader-name   (.getUploaderName info)
             :uploader-url    (.getUploaderUrl info)
             :uploader-avatar (.getUploaderAvatarUrl info)
             :stream-count    (.getStreamCount info)
             :next-page       (j/from-java (.getNextPage info))})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info
         (PlaylistInfo/getMoreItems service url (Page. (url-decode page-url)))]
     {:next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getItems info))})))

(defn get-comment-item
  [item extractor info]
  {:id                   (.getCommentId item)
   :text                 (.. item (getCommentText) (getContent))
   :upload-date          (.getTextualUploadDate item)
   :uploader-name        (.getUploaderName item)
   :uploader-url         (.getUploaderUrl item)
   :uploader-avatar      (.getUploaderAvatarUrl item)
   :uploader-verified?   (.isUploaderVerified item)
   :like-count           (when-not (= (.getLikeCount item) -1)
                           (.getLikeCount item))
   :reply-count          (when-not (= (.getReplyCount item) -1)
                           (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned?              (.isPinned item)
   :stream-position      (when-not (= (.getStreamPosition item) -1)
                           (.getStreamPosition item))
   :replies              (when (.getReplies item)
                           (if extractor
                             (let [comments-page (.getPage extractor
                                                           (.getReplies item))]
                               {:next-page (when (.hasNextPage comments-page)
                                             (j/from-java (.getNextPage
                                                           comments-page)))
                                :items     (map #(get-comment-item %
                                                                   extractor
                                                                   info)
                                                (.getItems comments-page))})
                             (j/from-java (.getReplies item))))})

(defn get-comments
  ([url]
   (let [info      (CommentsInfo/getInfo (url-decode url))
         extractor (.getCommentsExtractor info)]
     {:comments  (map #(get-comment-item % extractor info)
                      (.getRelatedItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? (.isCommentsDisabled info)}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info    (CommentsInfo/getMoreItems service
                                            (url-decode url)
                                            (Page. (url-decode page-url)))]
     {:comments  (map #(get-comment-item % nil info) (.getItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? false})))

(defn get-search
  ([service-id query content-filters sort-filter]
   (let [service (NewPipe/getService service-id)
         query-handler
         (.. service
             (getSearchQHFactory)
             (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getInfo service query-handler)]
     {:items             (get-items (.getRelatedItems info))
      :next-page         (j/from-java (.getNextPage info))
      :service-id        service-id
      :search-suggestion (.getSearchSuggestion info)
      :corrected-search? (.isCorrectedSearch info)}))
  ([service-id query content-filters sort-filter page-url]
   (let [service (NewPipe/getService service-id)
         url (url-decode page-url)
         query-handler
         (.. service
             (getSearchQHFactory)
             (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getMoreItems service query-handler (Page. url))]
     {:items     (get-items (.getItems info))
      :next-page (j/from-java (.getNextPage info))})))

(defn get-kiosk
  ([service-id]
   (let [service   (NewPipe/getService service-id)
         extractor (doto (.getDefaultKioskExtractor (.getKioskList service))
                     (.fetchPage))
         info      (KioskInfo/getInfo extractor)]
     {:id              (.getId info)
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([kiosk-id service-id]
   (let [service (NewPipe/getService service-id)
         extractor
         (doto (.getExtractorById (.getKioskList service) kiosk-id nil)
           (.fetchPage))
         info (KioskInfo/getInfo extractor)]
     {:id              (.getId info)
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([kiosk-id service-id page-url]
   (let [service    (NewPipe/getService service-id)
         extractor  (.getExtractorById (.getKioskList service) kiosk-id nil)
         url        (url-decode page-url)
         kiosk-info (KioskInfo/getInfo extractor)
         info       (KioskInfo/getMoreItems service
                                            (.getUrl kiosk-info)
                                            (Page. url))]
     {:next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getItems info))})))

(defn get-kiosks
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks  (.getKioskList service)]
    {:default-kiosk    (.getDefaultKioskId kiosks)
     :available-kiosks (.getAvailableKiosks kiosks)}))

(defn get-service
  [service]
  {:id       (.getServiceId service)
   :info     (j/from-java (.getServiceInfo service))
   :base-url (.getBaseUrl service)})

(defn get-services
  []
  (map get-service (NewPipe/getServices)))
