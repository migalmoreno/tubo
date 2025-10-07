(ns tubo.handlers.feed
  (:require
   [clojure.data.json :as json]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.channel :as channel]
   [tubo.models.subscription :as subscription]
   [tubo.handlers.utils :as utils]))

(defn get-channels-latest-streams
  [channels req]
  (->> channels
       (map #(-> (assoc-in req [:path-params :url] (:url %))
                 channel/get-channel-tab-info
                 (utils/->ListInfo req)
                 :related-items))
       flatten
       (remove nil?)
       (sort-by :upload-date #(> %1 %2))))

(defn create-get-user-feed-handler
  [req]
  (let [channels (subscription/get-subscriptions-by-user req)]
    (when (seq channels)
      (ok {:items    (get-channels-latest-streams channels req)
           :channels (map :url channels)}))))

(defn create-get-feed-handler
  [{:keys [query-params] :as req}]
  (let [urls (json/read-str (get query-params "channels"))]
    (when (seq urls)
      (ok {:items    (get-channels-latest-streams (map (fn [url] {:url url})
                                                       urls)
                                                  req)
           :channels urls}))))
