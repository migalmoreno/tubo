(ns tubo.models.playlist
  (:require
   [tubo.db :as db]
   [tubo.models.channel :as channel]
   [tubo.models.stream :as stream]))

(defn get-playlist-streams
  [id ds]
  (->> (db/execute!
        ds
        {:select  [:*]
         :from    [:streams]
         :join-by [:join
                   [:channels
                    [:= :channels.id :streams.channel_id]]
                   :join
                   [:playlist_streams
                    [:= :streams.id :playlist_streams.stream_id]]
                   :join
                   [:playlists
                    [:= :playlists.id :playlist_streams.playlist_id]]]
         :where   [:= :playlists.playlist_id (parse-uuid id)]}
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
  (into []
        (map #(assoc % :items (get-playlist-streams (str (:playlist_id %)) ds)))
        (db/plan ds
                 {:select [:*]
                  :from   [:playlists]
                  :where  [:= :owner id]})))

(defn get-playlist-by-id
  [id ds]
  (let [playlist (db/execute-one! ds
                                  {:select [:*]
                                   :from   [:playlists]
                                   :where  [:= :id id]})
        streams  (get-playlist-streams (str (:playlist-id playlist)) ds)]
    (assoc playlist :items streams)))

(defn get-playlist-by-playlist-id
  [id ds]
  (when-let [playlist (db/execute-one! ds
                                       {:select [:*]
                                        :from   [:playlists]
                                        :where  [:= :playlist_id
                                                 (parse-uuid id)]})]
    (assoc playlist
           :items
           (get-playlist-streams (str (:playlist-id playlist)) ds))))

(defn add-playlists
  [values ds]
  (db/execute! ds
               {:insert-into [:playlists]
                :columns     [:name :owner]
                :values      values}))

(defn add-playlist-streams
  [values ds]
  (db/execute! ds
               {:insert-into [:playlist_streams]
                :columns     [:stream_id :playlist_id]
                :values      values}))

(defn delete-playlist-streams-by-ids
  [playlist-id stream-ids ds]
  (db/execute-one! ds
                   {:delete-from [:playlist_streams]
                    :where       [:and [:= :playlist_id playlist-id]
                                  [:in :stream_id stream-ids]]}))

(defn delete-playlist-streams-by-playlist-id
  [playlist-id ds]
  (db/execute-one! ds
                   {:delete-from [:playlist_streams]
                    :where       [:= :playlist_id playlist-id]}))

(defn update-playlist
  [id values ds]
  (db/execute-one! ds
                   {:update [:playlists]
                    :set    values
                    :where  [:= :playlist_id (parse-uuid id)]}))

(defn delete-playlist
  [id ds]
  (let [playlist          (get-playlist-by-playlist-id id ds)
        unique-stream-ids (->> (stream/get-all-unique-streams-for-playlist
                                ds
                                (:id playlist))
                               (map :stream-id))]
    (println "channels")
    (delete-playlist-streams-by-playlist-id (:id playlist) ds)
    (when (seq unique-stream-ids)
      (let [unique-channel-ids
            (map :channel-id
                 (stream/get-unique-streams-channels ds unique-stream-ids))]
        (stream/delete-streams-by-ids ds unique-stream-ids)
        (channel/delete-channels-by-ids ds unique-channel-ids)))
    (db/execute-one! ds
                     {:delete-from [:playlists]
                      :where       [:= :id (:id playlist)]})))
