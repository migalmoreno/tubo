(ns tubo.handlers.auth-playlists
  (:require
   [clojure.data :as data]
   [ring.util.http-response :refer [ok]]
   [tubo.models.channel :as channel]
   [tubo.models.playlist :as playlist]
   [tubo.models.stream :as stream]))

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
    (playlist/add-playlist-streams [[(:id stream) (:id playlist) (:order item)]]
                                   ds)))

(defn create-get-auth-playlists-handler
  [{:keys [datasource identity]}]
  (ok (playlist/get-playlists-by-owner (:id identity) datasource)))

(defn create-get-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/get-full-playlist-by-playlist-id (:id path-params) datasource)))

(defn create-post-auth-playlists-handler
  [{:keys [datasource body-params identity]}]
  (let [{:keys [playlist-id] :as added-playlist}
        (first (playlist/add-playlists [[(:name body-params)
                                         (:id identity)
                                         (:thumbnail body-params)]]
                                       datasource))]
    (when (seq (:items body-params))
      (doseq [item (map-indexed (fn [i item] (assoc item :order (inc i)))
                                (:items body-params))]
        (add-playlist-streams datasource item (str playlist-id))))
    (ok (assoc added-playlist
               :id    playlist-id
               :items (playlist/get-playlist-streams (str playlist-id)
                                                     datasource)))))

(defn create-delete-auth-playlists-handler
  [{:keys [datasource identity]}]
  (ok (playlist/delete-owner-playlists datasource (:id identity))))

(defn create-delete-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/delete-playlist-by-id datasource (:id path-params))))

(defn create-post-auth-playlist-add-streams-handler
  [{:keys [datasource body-params path-params]}]
  (let [playlist-streams (playlist/get-playlist-streams (:id path-params)
                                                        datasource)
        items            (remove
                          nil?
                          (map-indexed
                           (fn [i item]
                             (when-let [filtered (first (filter #(= item
                                                                    (:url %))
                                                                body-params))]
                               (assoc filtered :order (inc i))))
                           (distinct (into (into [] (map :url playlist-streams))
                                           (into [] (map :url body-params))))))
        streams-to-add   (->> (data/diff
                               (into #{}
                                     (map :url playlist-streams))
                               (into #{} (map :url items)))
                              (second)
                              (map (fn [url]
                                     (first (filter #(= (:url %) url) items))))
                              (into []))]
    (when (seq streams-to-add)
      (doseq [item streams-to-add]
        (add-playlist-streams datasource item (:id path-params))))
    (ok (into [] streams-to-add))))

(defn create-post-auth-playlist-delete-stream-handler
  [{:keys [datasource body-params path-params]}]
  (let [playlist          (playlist/get-playlist-by-playlist-id
                           (:id path-params)
                           datasource)
        playlist-streams  (playlist/get-playlist-streams (:id path-params)
                                                         datasource)
        selected          (first (filter #(= (:url body-params)
                                             (:url %))
                                         playlist-streams))
        idx               (when selected
                            (.indexOf playlist-streams selected))
        unique-stream-ids (when selected
                            (->>
                              (stream/get-unique-streams-for-playlist
                               datasource
                               (:id playlist)
                               [(:id selected)])
                              (map :stream-id)))]
    (when (seq selected)
      (playlist/delete-playlist-streams-by-ids (:id playlist)
                                               [(:id selected)]
                                               datasource)
      (playlist/update-playlist-streams-order datasource (:id playlist) idx))
    (when (seq unique-stream-ids)
      (playlist/delete-unique-streams-by-ids datasource unique-stream-ids))
    (ok selected)))

(defn create-update-auth-playlist-handler
  [{:keys [datasource body-params path-params]}]
  (ok (playlist/update-playlist datasource (:id path-params) body-params)))
