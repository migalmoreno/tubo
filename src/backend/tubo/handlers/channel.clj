(ns tubo.handlers.channel
  (:require
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.channel.ChannelInfo
   org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo))

(defn create-channel-handler
  [{{:keys [url]} :path-params :as req}]
  (when-let [info (ChannelInfo/getInfo (url-decode url))]
    (-> (utils/->Info info)
        (update :banners #(utils/proxy-images % req))
        (update :avatars #(utils/proxy-images % req))
        (update :subscriber-count #(when (pos-int? %) %))
        ok)))

(defn get-channel-tab-info
  [{{:keys [url tab-id]} :path-params
    {:strs [nextPage]}   :query-params}]
  (let [url* (url-decode url)
        info (ChannelInfo/getInfo url*)
        tab  (if tab-id
               (->> (.getTabs info)
                    (filter #(some #{tab-id} (.getContentFilters %)))
                    first)
               (first (.getTabs info)))]
    (when tab
      (if nextPage
        (ChannelTabInfo/getMoreItems
         (NewPipe/getServiceByUrl url*)
         tab
         (utils/create-page nextPage))
        (ChannelTabInfo/getInfo (NewPipe/getServiceByUrl url*) tab)))))

(defn create-channel-tab-handler
  [req]
  (when-let [info (get-channel-tab-info req)]
    (ok (utils/->ListInfo info req))))
