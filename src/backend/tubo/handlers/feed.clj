(ns tubo.handlers.feed
  (:require
   [clojure.data.json :as json]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.channel :refer [get-channel]]
   [tubo.models.subscription :as subscription]))

(defn get-channels-latest-streams
  [channels]
  (->> channels
       (map #(-> %
                 :url
                 get-channel
                 :related-streams))
       flatten
       (remove nil?)
       (sort-by :uploaded #(> %1 %2))))

(defn create-get-user-feed-handler
  [{:keys [datasource identity]}]
  (let [channels (subscription/get-subscriptions-by-user datasource
                                                         (:id identity))]
    (when (seq channels)
      (ok {:items    (get-channels-latest-streams channels)
           :channels (map :url channels)}))))

(defn create-get-feed-handler
  [{:keys [query-params]}]
  (let [urls (json/read-str (get query-params "channels"))]
    (when (seq urls)
      (ok {:items    (get-channels-latest-streams (map (fn [url] {:url url})
                                                       urls))
           :channels urls}))))
