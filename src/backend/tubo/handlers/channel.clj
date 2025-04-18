(ns tubo.handlers.channel
  (:require
   [clojure.java.data :refer [from-java]]
   [ring.util.response :refer [response]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :refer [non-negative get-items]])
  (:import
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.linkhandler.ReadyChannelTabListLinkHandler
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo))

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
      :tabs             (from-java (.getTabs info))
      :verified?        (.isVerified info)
      :banners          (from-java (.getBanners info))
      :description      (.getDescription info)
      :avatars          (from-java (.getAvatars info))
      :subscriber-count (non-negative (.getSubscriberCount info))
      :donation-links   (.getDonationLinks info)
      :next-page        (from-java (.getNextPage tab-info))
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
      :next-page       (from-java (.getNextPage tab-info))})))

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
      :next-page       (from-java (.getNextPage info))}))
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
      :next-page       (from-java (.getNextPage info))})))

(defn create-channel-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply get-channel url (if nextPage [nextPage] []))))

(defn create-channel-tabs-handler
  [{{:keys [url tab-id]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply get-channel-tab url tab-id (if nextPage [nextPage] []))))
