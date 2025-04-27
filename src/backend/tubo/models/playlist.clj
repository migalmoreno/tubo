(ns tubo.models.playlist
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn get-playlist-streams
  [id ds]
  (->> (jdbc/execute!
        ds
        (-> {:select  [:*]
             :from    [:streams]
             :join-by [:join
                       [:channels
                        [:= :channels.url :streams.uploader_url]]
                       :join
                       [:playlist_streams
                        [:= :streams.id :playlist_streams.stream_id]]
                       :join
                       [:playlists
                        [:= :playlists.id :playlist_streams.playlist_id]]]
             :where   [:= :playlists.playlist_id (parse-uuid id)]}
            sql/format))
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
        (map #(let [streams (get-playlist-streams (str (:playlist_id %)) ds)]
                (assoc % :items streams)))
        (jdbc/plan ds
                   (-> {:select [:*]
                        :from   [:playlists]
                        :where  [:= :owner id]}
                       sql/format)
                   {:builder-fn rs/as-unqualified-kebab-maps})))

(defn get-playlist-by-id
  [id ds]
  (let [playlist (jdbc/execute-one! ds
                                    (-> {:select [:*]
                                         :from   [:playlists]
                                         :where  [:= :id id]}
                                        sql/format)
                                    {:return-keys true
                                     :builder-fn  rs/as-unqualified-kebab-maps})
        streams  (get-playlist-streams (str (:playlist-id playlist)) ds)]
    (assoc playlist :items streams)))

(defn get-playlist-by-playlist-id
  [id ds]
  (let [playlist (jdbc/execute-one! ds
                                    (-> {:select [:*]
                                         :from   [:playlists]
                                         :where  [:= :playlist_id
                                                  (parse-uuid id)]}
                                        sql/format)
                                    {:builder-fn rs/as-unqualified-kebab-maps})
        streams  (get-playlist-streams (str (:playlist-id playlist)) ds)]
    (assoc playlist :items streams)))

(defn add-playlists
  [values ds]
  (jdbc/execute! ds
                 (-> {:insert-into [:playlists]
                      :columns     [:name :owner]
                      :values      values}
                     sql/format)
                 {:return-keys true
                  :builder-fn  rs/as-unqualified-kebab-maps}))

(defn add-playlist-streams
  [values ds]
  (jdbc/execute! ds
                 (-> {:insert-into [:playlist_streams]
                      :columns     [:stream_id :playlist_id]
                      :values      values}
                     sql/format)
                 {:return-keys true
                  :builder-fn  rs/as-unqualified-kebab-maps}))

(defn delete-playlist-streams
  [playlist-id stream-ids ds]
  (jdbc/execute-one! ds
                     (-> {:delete-from [:playlist_streams]
                          :where       [:and [:= :playlist_id playlist-id]
                                        [:in :stream_id stream-ids]]}
                         sql/format)
                     {:return-keys true
                      :builder-fn  rs/as-unqualified-kebab-maps}))

(defn update-playlist
  [id values ds]
  (jdbc/execute-one! ds
                     (-> {:update [:playlists]
                          :set    values
                          :where  [:= :playlist_id (parse-uuid id)]}
                         sql/format)
                     {:return-keys true
                      :builder-fn  rs/as-unqualified-kebab-maps}))

(defn delete-playlist
  [id ds]
  (jdbc/execute-one! ds
                     (-> {:delete-from [:playlists]
                          :where       [:= :playlist_id (parse-uuid id)]}
                         sql/format)
                     {:return-keys true
                      :builder-fn  rs/as-unqualified-kebab-maps}))
