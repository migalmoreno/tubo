(ns tau.api.channels
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tau.api.items :as items])
  (:import
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defn get-channel
  ([url]
   (let [info (ChannelInfo/getInfo (url-decode url))]
     {:id (.getId info)
      :name (.getName info)
      :verified? (.isVerified info)
      :banner (.getBannerUrl info)
      :avatar (.getAvatarUrl info)
      :description (.getDescription info)
      :subscriber-count (when-not (= (.getSubscriberCount info) -1) (.getSubscriberCount info))
      :donation-links (.getDonationLinks info)
      :next-page (j/from-java (.getNextPage info))
      :related-streams (items/get-items (.getRelatedItems info))
      :service-id (.getServiceId info)}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (ChannelInfo/getMoreItems service (url-decode url) (Page. (url-decode page-url)))]
     {:related-streams (items/get-items (.getItems info))
      :next-page (j/from-java (.getNextPage info))})))
