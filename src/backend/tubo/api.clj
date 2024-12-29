(ns tubo.api
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   java.util.Locale
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.localization.ContentCountry
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.search.SearchInfo
   org.schabi.newpipe.extractor.stream.StreamInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler))

(defn non-negative
  [val]
  (when-not (= val -1) val))

(defn get-stream-item
  [stream]
  {:type              :stream
   :service-id        (.getServiceId stream)
   :url               (.getUrl stream)
   :name              (.getName stream)
   :thumbnails        (j/from-java (.getThumbnails stream))
   :uploader-name     (.getUploaderName stream)
   :uploader-url      (.getUploaderUrl stream)
   :uploader-avatars  (j/from-java (.getUploaderAvatars stream))
   :upload-date       (.getTextualUploadDate stream)
   :short-description (.getShortDescription stream)
   :duration          (.getDuration stream)
   :view-count        (non-negative (.getViewCount stream))
   :uploaded          (when (.getUploadDate stream)
                        (.. stream
                            (getUploadDate)
                            (offsetDateTime)
                            (toInstant)
                            (toEpochMilli)))
   :verified?         (.isUploaderVerified stream)})

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
   :stream-count  (non-negative (.getStreamCount playlist))})

(defn get-items
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM"   (get-stream-item %)
          "CHANNEL"  (get-channel-item %)
          "PLAYLIST" (get-playlist-item %))
       items))

(defn get-channel
  ([url]
   (let [info       (ChannelInfo/getInfo (url-decode url))
         service    (NewPipe/getServiceByUrl (url-decode url))
         videos-tab (->> (.getTabs info)
                         (filter #(instance?
                                   ReadyChannelTabListLinkHandler
                                   %))
                         (filter #(some #{ChannelTabs/VIDEOS}
                                        (.getContentFilters %)))
                         first)
         tab-info   (ChannelTabInfo/getInfo
                     service
                     (or videos-tab
                         (first (.getTabs info))))]
     {:name             (.getName info)
      :service-id       (.getServiceId info)
      :id               (.getId info)
      :tabs             (j/from-java (.getTabs info))
      :verified?        (.isVerified info)
      :banners          (j/from-java (.getBanners info))
      :description      (.getDescription info)
      :avatars          (j/from-java (.getAvatars info))
      :subscriber-count (non-negative (.getSubscriberCount info))
      :donation-links   (.getDonationLinks info)
      :next-page        (j/from-java (.getNextPage tab-info))
      :related-streams  (when tab-info
                          (get-items (.getRelatedItems tab-info)))}))
  ([url page-url]
   (let [channel-info (ChannelInfo/getInfo (url-decode url))
         service      (NewPipe/getServiceByUrl (url-decode url))
         videos-tab   (->> (.getTabs channel-info)
                           (filter #(instance?
                                     ReadyChannelTabListLinkHandler
                                     %))
                           (filter #(some #{ChannelTabs/VIDEOS}
                                          (.getContentFilters %)))
                           first)
         tab-info     (ChannelTabInfo/getMoreItems
                       service
                       (or videos-tab (first (.getTabs channel-info)))
                       (Page. (url-decode
                               page-url)))]
     {:related-streams (get-items (.getRelatedItems tab-info))
      :next-page       (j/from-java (.getNextPage tab-info))})))

(defn get-channel-tab
  ([url tabId]
   (let [service      (NewPipe/getServiceByUrl (url-decode url))
         channel-info (ChannelInfo/getInfo (url-decode url))
         tab          (if (= tabId "default")
                        (first (.getTabs channel-info))
                        (->> (.getTabs channel-info)
                             (filter #(some #{tabId} (.getContentFilters %)))
                             first))
         info         (ChannelTabInfo/getInfo service tab)]
     {:related-streams (get-items (.getRelatedItems info))
      :next-page       (j/from-java (.getNextPage info))}))
  ([url tabId page-url]
   (let [service      (NewPipe/getServiceByUrl (url-decode url))
         channel-info (ChannelInfo/getInfo (url-decode url))
         tab          (if (= tabId "default")
                        (first (.getTabs channel-info))
                        (->> (.getTabs channel-info)
                             (filter #(some #{tabId} (.getContentFilters %)))
                             first))
         info         (ChannelTabInfo/getMoreItems service
                                                   tab
                                                   (Page. (url-decode
                                                           page-url)))]
     {:related-streams (get-items (.getItems info))
      :next-page       (j/from-java (.getNextPage info))})))

(defn get-stream
  [url]
  (let [info (StreamInfo/getInfo (url-decode url))]
    {:name               (.getName info)
     :service-id         (.getServiceId info)
     :related-streams    (get-items (.getRelatedItems info))
     :url                (.getUrl info)
     :thumbnails         (j/from-java (.getThumbnails info))
     :description        (.. info (getDescription) (getContent))
     :duration           (.getDuration info)
     :upload-date        (.getTextualUploadDate info)
     :uploader-name      (.getUploaderName info)
     :uploader-url       (.getUploaderUrl info)
     :uploader-avatars   (j/from-java (.getUploaderAvatars info))
     :uploader-verified? (.isUploaderVerified info)
     :tags               (.getTags info)
     :category           (.getCategory info)
     :view-count         (non-negative (.getViewCount info))
     :like-count         (non-negative (.getLikeCount info))
     :dislike-count      (non-negative (.getDislikeCount info))
     :subscriber-count   (non-negative (.getUploaderSubscriberCount info))
     :audio-streams      (j/from-java (.getAudioStreams info))
     :video-streams      (j/from-java (.getVideoStreams info))
     :video-only-streams (j/from-java (.getVideoOnlyStreams info))
     :hls-url            (.getHlsUrl info)
     :dash-mpd-url       (.getDashMpdUrl info)
     :preview-frames     (j/from-java (.getPreviewFrames info))
     :stream-segments    (j/from-java (.getStreamSegments info))
     :support-info       (.getSupportInfo info)
     :short?             (.isShortFormContent info)
     :license            (.getLicence info)
     :subtitles          (j/from-java (.getSubtitles info))}))

(defn get-playlist
  ([url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info    (PlaylistInfo/getInfo service (url-decode url))]
     {:name             (.getName info)
      :service-id       (.getServiceId info)
      :related-streams  (get-items (.getRelatedItems info))
      :id               (.getId info)
      :playlist-type    (j/from-java (.getPlaylistType info))
      :thumbnails       (j/from-java (.getThumbnails info))
      :banners          (j/from-java (.getBanners info))
      :uploader-name    (.getUploaderName info)
      :uploader-url     (.getUploaderUrl info)
      :uploader-avatars (j/from-java (.getUploaderAvatars info))
      :stream-count     (.getStreamCount info)
      :next-page        (j/from-java (.getNextPage info))}))
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
   :uploader-avatars     (j/from-java (.getUploaderAvatars item))
   :uploader-verified?   (.isUploaderVerified item)
   :creator-reply?       (.hasCreatorReply item)
   :channel-owner?       (.isChannelOwner item)
   :like-count           (non-negative (.getLikeCount item))
   :reply-count          (non-negative (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned?              (.isPinned item)
   :stream-position      (non-negative (.getStreamPosition item))
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
  ([service-id query {:keys [filter sort]}]
   (let [service (NewPipe/getService service-id)
         query-handler
         (.. service
             (getSearchQHFactory)
             (fromQuery query (or filter '()) (or sort "")))
         info (SearchInfo/getInfo service query-handler)]
     {:items             (get-items (.getRelatedItems info))
      :next-page         (j/from-java (.getNextPage info))
      :service-id        service-id
      :search-suggestion (.getSearchSuggestion info)
      :corrected-search? (.isCorrectedSearch info)}))
  ([service-id query {:keys [filter sort]} page-url]
   (let [service (NewPipe/getService service-id)
         url (url-decode page-url)
         query-handler
         (.. service
             (getSearchQHFactory)
             (fromQuery query (or filter '()) (or sort "")))
         info (SearchInfo/getMoreItems service query-handler (Page. url))]
     {:items     (get-items (.getItems info))
      :next-page (j/from-java (.getNextPage info))})))

(defn get-kiosk
  ([{:keys [region]} service-id]
   (let [service   (NewPipe/getService service-id)
         extractor (doto (.getDefaultKioskExtractor
                          (if region
                            (doto (.getKioskList service)
                              (.forceContentCountry (ContentCountry. region)))
                            (.getKioskList service)))
                     (.fetchPage))
         info      (KioskInfo/getInfo extractor)]
     {:id              (.getId info)
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([{:keys [region]} kiosk-id service-id]
   (let [service (NewPipe/getService service-id)
         extractor
         (doto (.getExtractorById
                (if region
                  (doto (.getKioskList service)
                    (.forceContentCountry (ContentCountry. region)))
                  (.getKioskList service))
                kiosk-id
                nil)
           (.fetchPage))
         info (KioskInfo/getInfo extractor)]
     {:id              (.getId info)
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([{:keys [region]} kiosk-id service-id page-url]
   (let [service    (NewPipe/getService service-id)
         extractor  (.getExtractorById
                     (if region
                       (doto (.getKioskList service)
                         (.forceContentCountry (ContentCountry. region)))
                       (.getKioskList service))
                     kiosk-id
                     nil)
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
  {:id                  (.getServiceId service)
   :info                (j/from-java (.getServiceInfo service))
   :base-url            (.getBaseUrl service)
   :supported-languages (map (fn [lang]
                               {:name (.getDisplayLanguage
                                       (Locale. (.getLanguageCode lang)
                                                (.getCountryCode lang)))
                                :code (.getLocalizationCode lang)})
                             (.getSupportedLocalizations service))
   :supported-countries (map (fn [country]
                               {:name (.getDisplayCountry
                                       (Locale. "" (.toString country)))
                                :code (.toString country)})
                             (.getSupportedCountries service))
   :content-filters     (j/from-java (.. service
                                         (getSearchQHFactory)
                                         (getAvailableContentFilter)))})

(defn get-services
  []
  (map get-service (NewPipe/getServices)))
