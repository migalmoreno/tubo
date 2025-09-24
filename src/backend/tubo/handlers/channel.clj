(ns tubo.handlers.channel
  (:require
   [clojure.java.data :as j]
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
   org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo))

(defn get-channel
  [{{:keys [url]} :path-params :as req}]
  (let [info       (ChannelInfo/getInfo (url-decode url))
        service    (NewPipe/getServiceByUrl (url-decode url))
        videos-tab (->> (.getTabs info)
                        (filter #(instance?
                                  ReadyChannelTabListLinkHandler
                                  %))
                        (filter #(some #{ChannelTabs/VIDEOS}
                                       (.getContentFilters %)))
                        first)
        tab-info   (when (or videos-tab (first (.getTabs info)))
                     (ChannelTabInfo/getInfo
                      service
                      (or videos-tab
                          (first (.getTabs info)))))]
    {:name             (.getName info)
     :service-id       (.getServiceId info)
     :id               (.getId info)
     :tags             (.getTags info)
     :url              url
     :tabs             (j/from-java (.getTabs info))
     :verified?        (.isVerified info)
     :banners          (utils/proxy-images (.getBanners info) req)
     :description      (.getDescription info)
     :avatars          (utils/proxy-images (.getAvatars info) req)
     :subscriber-count (utils/non-negative (.getSubscriberCount info))
     :feed-url         (.getFeedUrl info)
     :donation-links   (.getDonationLinks info)
     :next-page        (when tab-info (utils/get-next-page tab-info))
     :related-streams  (when tab-info
                         (utils/get-items (.getRelatedItems tab-info) req))}))

(defn get-channel-page
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params :as req}]
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
                      (utils/create-page nextPage))]
    {:related-streams (utils/get-items (.getRelatedItems tab-info) req)
     :next-page       (utils/get-next-page tab-info)}))

(defn get-channel-tab
  [{{:keys [url tab-id]} :path-params :as req}]
  (let [service      (NewPipe/getServiceByUrl (url-decode url))
        channel-info (ChannelInfo/getInfo (url-decode url))
        tab          (if (= tab-id "default")
                       (first (.getTabs channel-info))
                       (->> (.getTabs channel-info)
                            (filter #(some #{tab-id} (.getContentFilters %)))
                            first))
        info         (ChannelTabInfo/getInfo service tab)]
    {:related-streams (utils/get-items (.getRelatedItems info) req)
     :next-page       (utils/get-next-page info)}))

(defn get-channel-tab-page
  [{{:keys [url tab-id]} :path-params {:strs [nextPage]} :query-params :as req}]
  (let [service      (NewPipe/getServiceByUrl (url-decode url))
        channel-info (ChannelInfo/getInfo (url-decode url))
        tab          (if (= tab-id "default")
                       (first (.getTabs channel-info))
                       (->>
                         (.getTabs channel-info)
                         (filter #(some #{tab-id} (.getContentFilters %)))
                         first))
        info         (ChannelTabInfo/getMoreItems
                      service
                      tab
                      (utils/create-page nextPage))]
    {:related-streams (utils/get-items (.getItems info) req)
     :next-page       (utils/get-next-page info)}))

(defn create-channel-handler
  [{{:strs [nextPage]} :query-params :as req}]
  (ok (if nextPage (get-channel-page req) (get-channel req))))

(defn create-channel-tabs-handler
  [{{:strs [nextPage]} :query-params :as req}]
  (ok (if nextPage (get-channel-tab-page req) (get-channel-tab req))))
