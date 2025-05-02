(ns tubo.models.playlist
  (:require
   [tubo.db :as db]
   [tubo.models.channel :as channel]
   [tubo.models.stream :as stream]))

(defn get-playlist-streams
  [id ds]
  (->> (db/execute!
        ds
        {:select   [:*]
         :from     [:streams]
         :join-by  [:join
                    [:channels
                     [:= :channels.id :streams.channel_id]]
                    :join
                    [:playlist_streams
                     [:= :streams.id :playlist_streams.stream_id]]
                    :join
                    [:playlists
                     [:= :playlists.id :playlist_streams.playlist_id]]]
         :where    [:= :playlists.playlist_id (parse-uuid id)]
         :order-by [:playlist_streams.playlist_stream_order]}
        {})
       (map (fn [e]
              {:id                 (:streams/id e)
               :name               (:streams/name e)
               :url                (:streams/url e)
               :duration           (:streams/duration e)
               :thumbnail          (:streams/thumbnail e)
               :uploader-verified? (:channels/verified e)
               :uploader-name      (:channels/name e)
               :uploader-url       (:channels/url e)
               :uploader-avatar    (:channels/avatar e)}))))

(defn get-playlists-by-owner
  [id ds]
  (into
   []
   (map #(-> (assoc % :items (get-playlist-streams (str (:playlist_id %)) ds))))
   (db/plan ds
            {:select [:*]
             :from   [:playlists]
             :where  [:= :owner id]})))

(defn get-full-playlist-by-playlist-id
  [id ds]
  (when-let [playlist (db/execute-one! ds
                                       {:select [:*]
                                        :from   [:playlists]
                                        :where  [:= :playlist_id
                                                 (parse-uuid id)]})]
    (assoc playlist
           :items
           (get-playlist-streams (str (:playlist-id playlist)) ds))))

(defn get-playlist-by-playlist-id
  [id ds]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:playlists]
                    :where  [:= :playlist_id (parse-uuid id)]}))

(defn add-playlists
  [values ds]
  (db/execute! ds
               {:insert-into [:playlists]
                :columns     [:name :owner :thumbnail]
                :values      values}))

(defn add-playlist-streams
  [values ds]
  (db/execute! ds
               {:insert-into [:playlist_streams]
                :columns     [:stream_id :playlist_id :playlist_stream_order]
                :values      values}))

(defn delete-playlist-streams-by-ids
  [playlist-id stream-ids ds]
  (db/execute-one! ds
                   {:delete-from [:playlist_streams]
                    :where       [:and [:= :playlist_id playlist-id]
                                  [:in :stream_id stream-ids]]}))

(defn delete-playlist-streams-by-playlist-ids
  [playlist-ids ds]
  (db/execute-one! ds
                   {:delete-from [:playlist_streams]
                    :where       [:in :playlist_id playlist-ids]}))

(defn update-playlist
  [ds id values]
  (db/execute-one! ds
                   {:update [:playlists]
                    :set    values
                    :where  [:= :playlist_id (parse-uuid id)]}))

(defn delete-unique-streams-by-ids
  [ds ids]
  (when (seq ids)
    (let [channel-ids (map
                       :channel-id
                       (stream/get-unique-streams-channels-for-non-ids ds
                                                                       ids))]
      (stream/delete-streams-by-ids ds ids)
      (when (seq channel-ids)
        (channel/delete-channels-by-ids ds channel-ids)))))

(defn delete-playlist-by-id
  [ds playlist-id]
  (let [playlist          (get-playlist-by-playlist-id playlist-id ds)
        unique-stream-ids (->> (stream/get-all-unique-streams-for-playlists
                                ds
                                [(:id playlist)])
                               (map :stream-id))]
    (delete-playlist-streams-by-playlist-ids [(:id playlist)] ds)
    (delete-unique-streams-by-ids ds unique-stream-ids)
    (db/execute-one! ds
                     {:delete-from [:playlists]
                      :where       [:= :id (:id playlist)]})))

(defn delete-playlists-by-owner
  [ds id]
  (db/execute! ds
               {:delete-from [:playlists]
                :where       [:= :owner id]}))

(defn delete-owner-playlists
  [ds owner-id]
  (let [playlists-ids     (map :id (get-playlists-by-owner owner-id ds))
        unique-stream-ids (when (seq playlists-ids)
                            (->> (stream/get-all-unique-streams-for-playlists
                                  ds
                                  playlists-ids)
                                 (map :stream-id)))]
    (when (seq playlists-ids)
      (delete-playlist-streams-by-playlist-ids playlists-ids ds))
    (when (seq unique-stream-ids)
      (delete-unique-streams-by-ids ds unique-stream-ids))
    (delete-playlists-by-owner ds owner-id)))

(defn update-playlist-streams-order
  [ds playlist-id idx]
  (db/execute! ds
               {:update [:playlist_streams]
                :set    {:playlist_stream_order [:- :playlist_stream_order 1]}
                :where  [:and
                         [:= :playlist_id playlist-id]
                         [:> :playlist_stream_order idx]]}))
