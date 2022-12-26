(ns tau.api.channel
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tau.api.result :as result])
  (:import
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defrecord Channel
    [id name description verified? banner avatar
     subscriber-count donation-links next-page
     related-streams])

(defrecord ChannelPage
    [next-page related-streams])

(defn get-info
  ([url]
   (let [info (ChannelInfo/getInfo (url-decode url))]
     (map->Channel
      {:id (.getId info)
       :name (.getName info)
       :verified? (.isVerified info)
       :banner (.getBannerUrl info)
       :avatar (.getAvatarUrl info)
       :description (.getDescription info)
       :subscriber-count (if (= (.getSubscriberCount info) -1) false (.getSubscriberCount info))
       :donation-links (.getDonationLinks info)
       :next-page (j/from-java (.getNextPage info))
       :related-streams (result/get-results (.getRelatedItems info))})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (ChannelInfo/getMoreItems service (url-decode url) (Page. (url-decode page-url)))]
     (map->ChannelPage
      {:related-streams (result/get-results (.getItems info))
       :next-page (j/from-java (.getNextPage info))}))))
