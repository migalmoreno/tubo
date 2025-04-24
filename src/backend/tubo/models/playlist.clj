(ns tubo.models.playlist
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn get-playlists-by-owner
  [id ds]
  (into []
        (map #(-> (dissoc % :playlist-id)
                  (assoc :id (:playlist_id %))))
        (jdbc/plan ds
                   (-> {:select [:playlist_id :name :thumbnail]
                        :from   [:playlists]
                        :where  [:= :owner id]}
                       sql/format)
                   {:builder-fn rs/as-unqualified-kebab-maps})))

(defn get-playlist-by-playlist-id
  [id ds]
  (jdbc/execute-one! ds
                     (-> {:select [:*]
                          :from   [:playlists]
                          :where  [:= :playlist_id (parse-uuid id)]}
                         sql/format)
                     {:builder-fn rs/as-unqualified-kebab-maps}))

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

(defn get-playlist-streams
  [id ds]
  (jdbc/execute!
   ds
   (-> {:select  [:streams.*]
        :from    [:streams]
        :join-by [:join
                  [:playlist_streams
                   [:= :streams.id :playlist_streams.stream_id]]
                  :join
                  [:playlists [:= :playlists.id :playlist_streams.playlist_id]]]
        :where   [:= :playlists.playlist_id (parse-uuid id)]}
       sql/format)
   {:builder-fn rs/as-unqualified-kebab-maps}))

(defn update-playlist
  [id values ds]
  (jdbc/execute-one! ds
                     (-> {:update [:playlists]
                          :set    values
                          :where  [:= :playlist_id (parse-uuid id)]}
                         sql/format)))

(defn delete-playlist
  [id ds]
  (jdbc/execute-one! ds
                     (-> {:delete-from [:playlists]
                          :where       [:= :playlist_id (parse-uuid id)]}
                         sql/format)
                     {:return-keys true
                      :builder-fn  rs/as-unqualified-kebab-maps}))
