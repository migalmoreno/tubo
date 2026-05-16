(ns tubo.extractors.handlers
  (:require
   [clojure.java.data :as j]
   [clojure.string :as str]
   [ring.util.http-response :refer [internal-server-error ok]]
   [ring.util.codec :refer [url-decode]]
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
  [{{:keys [url]} :path-params :as req}]
  (when-let [info (ChannelInfo/getInfo (url-decode url))]
    (-> (utils/->Info info)
        (update :banners #(utils/proxy-images % req))
        (update :avatars #(utils/proxy-images % req))
        (update :subscriber-count #(when (pos-int? %) %))
        ok)))

(defn get-channel-tab-info
  [{:keys [path-params query-params]}]
  (let [url* (url-decode (:url path-params))
        info (ChannelInfo/getInfo url*)
        tab  (if-let [tab-id (:tab-id path-params)]
               (->> (.getTabs info)
                    (filter #(some #{tab-id} (.getContentFilters %)))
                    first)
               (first (.getTabs info)))]
    (when tab
      (if-let [next-page (:nextPage query-params)]
        (ChannelTabInfo/getMoreItems
         (NewPipe/getServiceByUrl url*)
         tab
         (utils/create-page next-page))
        (ChannelTabInfo/getInfo (NewPipe/getServiceByUrl url*) tab)))))

(defn create-channel-tab-handler
  [req]
  (when-let [info (get-channel-tab-info req)]
    (ok (utils/->ListInfo info req))))

(defn create-comments-handler
  [{:keys [path-params query-params] :as req}]
  (let [url* (url-decode (:url path-params))
        info (if (:nextPage query-params)
               (CommentsInfo/getMoreItems (NewPipe/getServiceByUrl url*)
                                          url*
                                          (utils/create-page (:nextPage
                                                              query-params)))
               (CommentsInfo/getInfo url*))]
    (when info
      (ok (utils/->ListInfo info req)))))

(defn get-kiosk
  [{:keys [path-params query-params]}]
  (let [service    (NewPipe/getService (:service-id path-params))
        kiosk-list (if-let [region (:region query-params)]
                     (doto (.getKioskList service)
                       (.forceContentCountry (ContentCountry. region)))
                     (.getKioskList service))]
    (-> (doto (if (and (:kiosk-id path-params) (:service-id path-params))
                (.getExtractorById kiosk-list (:kiosk-id path-params) nil)
                (.getDefaultKioskExtractor kiosk-list))
              (.fetchPage))
        KioskInfo/getInfo)))

(defn create-kiosks-handler
  [{:keys [path-params]}]
  (ok (utils/->Info (.getKioskList (NewPipe/getService (:service-id
                                                        path-params))))))

(defn create-kiosk-handler
  [{:keys [path-params query-params] :as req}]
  (when-let [info (if-let [next-page (:nextPage query-params)]
                    (KioskInfo/getMoreItems (NewPipe/getService (:service-id
                                                                 path-params))
                                            (.getUrl (get-kiosk req))
                                            (utils/create-page next-page))
                    (get-kiosk req))]
    (ok (utils/->ListInfo info req))))

(defn create-playlist-handler
  [{:keys [path-params query-params] :as req}]
  (let [url* (url-decode (:url path-params))
        info (if-let [next-page (:nextPage query-params)]
               (PlaylistInfo/getMoreItems (NewPipe/getServiceByUrl url*)
                                          url*
                                          (utils/create-page next-page))
               (PlaylistInfo/getInfo url*))]
    (when info
      (ok (utils/->ListInfo info req)))))

(defn get-query-handler
  [{:keys [path-params query-params]}]
  (.. (NewPipe/getService (:service-id path-params))
      (getSearchQHFactory)
      (fromQuery (:q query-params)
                 (or (and (seq (:filter query-params)) (str/split filter #","))
                     '())
                 (or sort ""))))

(defn create-search-handler
  [{:keys [path-params query-params] :as req}]
  (when-let [info (if-let [next-page (:nextPage query-params)]
                    (SearchInfo/getMoreItems (NewPipe/getService (:service-id
                                                                  path-params))
                                             (get-query-handler req)
                                             (utils/create-page next-page))
                    (SearchInfo/getInfo (NewPipe/getService (:service-id
                                                             path-params))
                                        (get-query-handler req)))]
    (ok (utils/->ListInfo info req))))

(defn create-suggestions-handler
  [{:keys [path-params query-params]}]
  (when-let [extractor (.getSuggestionExtractor (NewPipe/getService
                                                 (:service-id path-params)))]
    (ok (j/from-java-shallow (.suggestionList extractor (:q query-params))
                             {}))))

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

(defn create-services-handler
  [_]
  (when-let [services (NewPipe/getServices)]
    (->> (utils/->Info services)
         (map
          (fn [service]
            (-> service
                (update :supported-localizations
                        (fn [locales]
                          (map #(assoc %
                                       :name
                                       (.getDisplayLanguage
                                        (Locale. (:language-code %)
                                                 (:country-code %))))
                               locales)))
                (update :supported-countries
                        (fn [countries]
                          (map #(assoc %
                                       :name
                                       (.getDisplayCountry
                                        (Locale. "" (:country-code %))))
                               countries))))))
         ok)))

(defn create-instance-handler
  [_]
  (ok (j/from-java-shallow (.getInstance ServiceList/PeerTube) {})))

(defn fetch-instance-metadata
  [url]
  (j/from-java-shallow (doto (PeertubeInstance. (url-decode url))
                         (.fetchInstanceMetaData))
                       {}))

(defn create-instance-metadata-handler
  [{{:keys [url]} :path-params}]
  (ok (fetch-instance-metadata url)))

(defn create-change-instance-handler
  [{{:keys [url name]} :body-params}]
  (try
    (fetch-instance-metadata url)
    (.setInstance ServiceList/PeerTube (PeertubeInstance. url name))
    (ok (str "PeerTube instance changed to " name))
    (catch Exception _
      (internal-server-error
       "There was a problem changing PeerTube instance"))))

(def routes
  {:api/services {:get {:summary "returns all supported services"
                        :handler create-services-handler}}
   :api/search {:get {:summary
                      "returns search results for a given service"
                      :parameters {:path  {:service-id int?}
                                   :query {:q string?}}
                      :handler create-search-handler}}
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
                                 :handler create-instance-metadata-handler}}
   :api/change-instance {:post {:summary
                                "changes the instance for a given service"
                                :handler create-change-instance-handler
                                :parameters {:body s/PeerTubeInstance}}}
   :api/default-kiosk {:get
                       {:summary
                        "returns default kiosk entries for a given service"
                        :parameters {:path {:service-id int?}}
                        :handler create-kiosk-handler}}
   :api/all-kiosks {:get {:summary
                          "returns all kiosks supported by a given service"
                          :parameters {:path {:service-id int?}}
                          :handler create-kiosks-handler}}
   :api/kiosk {:get
               {:summary
                "returns kiosk entries for a given service and a kiosk ID"
                :parameters {:path {:service-id int? :kiosk-id string?}}
                :handler create-kiosk-handler}}
   :api/stream {:get {:summary    "returns stream data for a given URL"
                      :parameters {:path {:url uri?}}
                      :handler    create-stream-handler}}
   :api/channel {:get {:summary    "returns channel data for a given URL"
                       :parameters {:path {:url uri?}}
                       :handler    create-channel-handler}}
   :api/channel-tab {:get
                     {:summary
                      "returns channel tab data for a given URL and a tab ID"
                      :parameters {:path {:url uri? :tab-id string?}}
                      :handler create-channel-tab-handler}}
   :api/playlist {:get {:summary    "returns playlist data for a given URL"
                        :parameters {:path {:url uri?}}
                        :handler    create-playlist-handler}}
   :api/comments {:get {:summary    "returns comments data for a given URL"
                        :parameters {:path {:url uri?}}
                        :handler    create-comments-handler}}})
