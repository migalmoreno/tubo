(ns tubo.extractors.handlers
  (:require
   [clojure.java.data :as j]
   [clojure.string :as str]
   [ring.util.http-response :refer [internal-server-error not-found ok]]
   [tubo.schemas :as s]
   [tubo.utils :as utils])
  (:import
   java.util.Locale
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.ServiceList
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.localization.ContentCountry
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.search.SearchInfo
   org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
   org.schabi.newpipe.extractor.stream.StreamInfo))

(defn create-channel-handler
  [{:keys [parameters] :as req}]
  (-> (utils/->Info (ChannelInfo/getInfo (get-in parameters [:path :url])))
      (update :banners #(utils/proxy-images % req))
      (update :avatars #(utils/proxy-images % req))
      (update :subscriber-count #(when (pos-int? %) %))
      ok))

(defn get-channel-tab-info
  [{:keys [parameters]}]
  (let [{:keys [url tab-id]} (:path parameters)
        service              (NewPipe/getServiceByUrl url)
        tab                  (->> (ChannelInfo/getInfo url)
                                  (.getTabs)
                                  (filter #(some #{tab-id}
                                                 (.getContentFilters %)))
                                  first)]
    (when tab
      (if-let [next-page (get-in parameters [:query :nextPage])]
        (ChannelTabInfo/getMoreItems service tab (utils/->Page next-page))
        (ChannelTabInfo/getInfo service tab)))))

(defn create-channel-tab-handler
  [{:keys [parameters] :as req}]
  (if-let [info (get-channel-tab-info req)]
    (ok (utils/->ListInfo info req))
    (not-found (str "Tab " (get-in parameters [:path :tab-id]) " not found"))))

(defn create-comments-handler
  [{:keys [parameters] :as req}]
  (let [url  (get-in parameters [:path :url])
        info (if-let [next-page (get-in parameters [:query :nextPage])]
               (CommentsInfo/getMoreItems (NewPipe/getServiceByUrl url)
                                          url
                                          (utils/->Page next-page))
               (CommentsInfo/getInfo url))]
    (ok (utils/->ListInfo info req))))

(defn get-kiosk
  [{:keys [parameters]}]
  (let [service    (NewPipe/getService (get-in parameters [:path :service-id]))
        kiosk-list (if-let [region (get-in parameters [:query :region])]
                     (doto (.getKioskList service)
                       (.forceContentCountry (ContentCountry. region)))
                     (.getKioskList service))]
    (-> (doto (if-let [kiosk-id (get-in parameters [:path :kiosk-id])]
                (.getExtractorById kiosk-list kiosk-id nil)
                (.getDefaultKioskExtractor kiosk-list))
              (.fetchPage))
        KioskInfo/getInfo)))

(defn create-kiosks-handler
  [{:keys [parameters]}]
  (ok (utils/->Info (.getKioskList (NewPipe/getService
                                    (get-in parameters [:path :service-id]))))))

(defn create-kiosk-handler
  [{:keys [parameters] :as req}]
  (let [info (if-let [next-page (get-in parameters [:query :nextPage])]
               (KioskInfo/getMoreItems (NewPipe/getService
                                        (get-in parameters
                                                [:path :service-id]))
                                       (.getUrl (get-kiosk req))
                                       (utils/->Page next-page))
               (get-kiosk req))]
    (ok (utils/->ListInfo info req))))

(defn create-playlist-handler
  [{:keys [parameters] :as req}]
  (let [url  (get-in parameters [:path :url])
        info (if-let [next-page (get-in parameters [:query :nextPage])]
               (PlaylistInfo/getMoreItems (NewPipe/getServiceByUrl url)
                                          url
                                          (utils/->Page next-page))
               (PlaylistInfo/getInfo url))]
    (ok (utils/->ListInfo info req))))

(defn build-query-handler
  [{:keys [parameters]}]
  (.. (NewPipe/getService (get-in parameters [:path :service-id]))
      (getSearchQHFactory)
      (fromQuery (get-in parameters [:query :q])
                 (if-let [filter-query (get-in parameters [:query :filter])]
                   (str/split filter-query #",")
                   '())
                 (or (get-in parameters [:query :sort]) ""))))

(defn create-search-handler
  [{:keys [parameters] :as req}]
  (let [service (NewPipe/getService (get-in parameters [:path :service-id]))
        query   (build-query-handler req)
        info    (if-let [next-page (get-in parameters [:query :nextPage])]
                  (SearchInfo/getMoreItems service
                                           query
                                           (utils/->Page next-page))
                  (SearchInfo/getInfo service query))]
    (ok (utils/->ListInfo info req))))

(defn create-suggestions-handler
  [{:keys [parameters]}]
  (-> (NewPipe/getService (get-in parameters [:path :service-id]))
      (.getSuggestionExtractor)
      (.suggestionList (get-in parameters [:query :q]))
      (j/from-java-shallow {})
      ok))

(defn ->Streams
  [items items*]
  (map-indexed
   (fn [idx item]
     (assoc item :mime-type (.. (.get items* idx) getFormat getMimeType)))
   items))

(defn create-stream-handler
  [{:keys [parameters] :as req}]
  (let [info (StreamInfo/getInfo (get-in parameters [:path :url]))]
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

(defn create-services-handler
  [_]
  (->> (utils/->Info (NewPipe/getServices))
       (map
        (fn [service]
          (-> service
              (update :supported-localizations
                      #(map (fn [{:keys [language-code country-code] :as loc}]
                              (assoc loc
                                     :name
                                     (.getDisplayLanguage
                                      (Locale. language-code country-code))))
                            %))
              (update :supported-countries
                      #(map (fn [{:keys [country-code] :as country}]
                              (assoc country
                                     :name
                                     (.getDisplayCountry
                                      (Locale. "" country-code))))
                            %)))))
       ok))

(defn create-instance-handler
  [_]
  (ok (j/from-java-shallow (.getInstance ServiceList/PeerTube) {})))

(defn fetch-instance-metadata
  [url]
  (j/from-java-shallow (doto (PeertubeInstance. url) (.fetchInstanceMetaData))
                       {}))

(defn create-instance-metadata-handler
  [{:keys [parameters]}]
  (ok (fetch-instance-metadata (get-in parameters [:path :url]))))

(defn create-change-instance-handler
  [{:keys [parameters]}]
  (try
    (let [{:keys [url name]} (:body parameters)]
      (fetch-instance-metadata url)
      (.setInstance ServiceList/PeerTube (PeertubeInstance. url name))
      (ok (str "PeerTube instance changed to " name)))
    (catch Exception _
      (internal-server-error
       "There was a problem changing PeerTube instance"))))

(def paginated-query
  [:map [:nextPage {:optional true} string?]])

(def kiosk-query
  [:map
   [:region {:optional true} string?]
   [:nextPage {:optional true} string?]])

(def routes
  {:api/services {:get {:summary "returns all supported services"
                        :handler create-services-handler}}
   :api/search
   {:get {:summary    "returns search results for a given service"
          :parameters {:path  {:service-id int?}
                       :query [:map
                               [:q string?]
                               [:sort {:optional true} string?]
                               [:filter {:optional true} string?]
                               [:nextPage {:optional true} string?]]}
          :handler    create-search-handler}}
   :api/suggestions {:get {:summary
                           "returns search suggestions for a given service"
                           :parameters {:path  {:service-id int?}
                                        :query {:q string?}}
                           :handler create-suggestions-handler}}
   :api/instance {:get {:summary
                        "returns the current instance for a given service"
                        :handler create-instance-handler}}
   :api/instance-metadata {:get {:summary
                                 "returns instance metadata for a given service"
                                 :parameters {:path {:url string?}}
                                 :handler create-instance-metadata-handler}}
   :api/change-instance {:post {:summary
                                "changes the instance for a given service"
                                :handler create-change-instance-handler
                                :parameters {:body s/PeerTubeInstance}}}
   :api/default-kiosk {:get
                       {:summary
                        "returns default kiosk entries for a given service"
                        :parameters {:path  {:service-id int?}
                                     :query kiosk-query}
                        :handler create-kiosk-handler}}
   :api/all-kiosks {:get {:summary
                          "returns all kiosks supported by a given service"
                          :parameters {:path {:service-id int?}}
                          :handler create-kiosks-handler}}
   :api/kiosk {:get {:summary
                     "returns kiosk entries for a given service and a kiosk ID"
                     :parameters {:path  {:service-id int? :kiosk-id string?}
                                  :query kiosk-query}
                     :handler create-kiosk-handler}}
   :api/stream {:get {:summary    "returns stream data for a given URL"
                      :parameters {:path {:url string?}}
                      :handler    create-stream-handler}}
   :api/channel {:get {:summary    "returns channel data for a given URL"
                       :parameters {:path {:url string?}}
                       :handler    create-channel-handler}}
   :api/channel-tab {:get {:summary
                           "returns channel tab data for a given URL and tab ID"
                           :parameters
                           {:path  {:url string? :tab-id string?}
                            :query paginated-query}
                           :handler create-channel-tab-handler}}
   :api/playlist {:get {:summary    "returns playlist data for a given URL"
                        :parameters {:path  {:url string?}
                                     :query paginated-query}
                        :handler    create-playlist-handler}}
   :api/comments {:get {:summary    "returns comments data for a given URL"
                        :parameters {:path  {:url string?}
                                     :query paginated-query}
                        :handler    create-comments-handler}}})
