(ns tubo.handlers.auth-playlists
  (:require
   [clojure.data :as data]
   [ring.util.http-response :refer [bad-request ok]]
   [tubo.models.channel :as channel]
   [tubo.models.playlist :as playlist]
   [tubo.models.stream :as stream]))

(defn create-get-auth-playlists-handler
  [{:keys [datasource identity]}]
  (ok (playlist/get-playlists-by-owner (:id identity) datasource)))

(defn create-get-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/get-full-playlist-by-playlist-id (:id path-params) datasource)))

(defn create-post-auth-playlists-handler
  [{:keys [datasource body-params identity]}]
  (let [added-playlist (first (playlist/add-playlists [[(:name body-params)
                                                        (:id identity)]]
                                                      datasource))]
    (ok (assoc added-playlist :id (:playlist-id added-playlist)))))

(defn create-delete-auth-playlists-handler
  [{:keys [datasource identity]}]
  (ok (playlist/delete-owner-playlists datasource (:id identity))))

(defn create-delete-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/delete-playlist-by-id datasource (:id path-params))))

(defn add-playlist-streams
  [ds item playlist-id]
  (let [channel  (or (channel/get-channel-by-url (:uploader-url item)
                                                 ds)
                     (first (channel/add-channels
                             [(into []
                                    (map item
                                         [:uploader-url
                                          :uploader-name
                                          :uploader-avatar
                                          :uploader-verified?]))]
                             ds)))
        stream   (or (stream/get-stream-by-url (:url item) ds)
                     (first (stream/add-streams
                             [(into (map item [:name :thumbnail :url])
                                    [(:id channel) (:duration item)])]
                             ds)))
        playlist (playlist/get-playlist-by-playlist-id playlist-id ds)]
    (playlist/add-playlist-streams [[(:id stream) (:id playlist)
                                     (:order item)]]
                                   ds)))

(defn create-update-auth-playlist-handler
  [{:keys [datasource body-params path-params]}]
  (let [playlist-streams     (playlist/get-playlist-streams (:id path-params)
                                                            datasource)
        items                (map-indexed (fn [i item]
                                            (assoc item :order (inc i)))
                                          (:items body-params))
        diff-streams         (data/diff
                              (into #{}
                                    (map :url playlist-streams))
                              (into #{} (map :url items)))
        streams-to-add       (when (second diff-streams)
                               (into []
                                     (map (fn [url]
                                            (first (filter #(= (:url %) url)
                                                           items)))
                                          (second diff-streams))))
        stream-ids-to-delete (when (first diff-streams)
                               (map :id
                                    (stream/get-streams-by-urls
                                     datasource
                                     (first diff-streams))))
        unique-stream-ids    (when (seq stream-ids-to-delete)
                               (->>
                                 (stream/get-unique-streams-for-playlist
                                  datasource
                                  (:id body-params)
                                  stream-ids-to-delete)
                                 (map :stream-id)))]
    (try
      (when (seq streams-to-add)
        (doseq [item streams-to-add]
          (add-playlist-streams datasource item (:id path-params))))
      (when (seq stream-ids-to-delete)
        (playlist/delete-playlist-streams-by-ids (:id body-params)
                                                 stream-ids-to-delete
                                                 datasource))
      (when (seq unique-stream-ids)
        (playlist/delete-unique-streams-by-ids datasource unique-stream-ids))
      (ok (merge (if (seq (select-keys body-params [:name :thumbnail]))
                   (playlist/update-playlist datasource
                                             (:id path-params)
                                             (select-keys body-params
                                                          [:name :thumbnail]))
                   (playlist/get-playlist-by-playlist-id (:id path-params)
                                                         datasource))
                 {:items (:items body-params)}))
      (catch Exception e
        (bad-request (ex-message e))))))
