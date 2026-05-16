(ns tubo.queries
  (:require
   [tubo.db :as db]))

(defn add-streams
  [values ds]
  (db/execute! ds
               {:insert-into [:streams]
                :columns     [:duration :channel-id :name :thumbnail :url]
                :values      values}))

(defn get-stream-by-url
  [url ds]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:streams]
                    :where  [:= :url url]}))

(defn get-unique-streams-for-playlist
  [ds id stream-ids]
  (db/execute! ds
               {:select  [:stream-id]
                :from    [:playlist_streams]
                :join-by [:join
                          [:streams
                           [:= :streams.id :playlist_streams.stream_id]]
                          :join
                          [:playlists
                           [:= :playlists.id :playlist_streams.playlist_id]]]
                :where   [:and
                          [:= :playlists.id id]
                          [:in :streams.id stream-ids]
                          [:not-in
                           :streams.id
                           {:select [:stream_id]
                            :from   [:playlist_streams]
                            :where  [:<> :playlist_id id]}]]}))

(defn delete-streams-by-ids
  [ds stream-ids]
  (db/execute! ds
               {:delete-from [:streams]
                :where       [:in :id stream-ids]}))

(defn get-all-unique-streams-for-playlists
  [ds playlist-ids]
  (db/execute!
   ds
   {:select  [:stream-id]
    :from    [:playlist_streams]
    :join-by [:join
              [:streams
               [:= :streams.id :playlist_streams.stream_id]]
              :join
              [:playlists
               [:= :playlists.id :playlist_streams.playlist_id]]]
    :where   [:and
              [:in :playlists.id playlist-ids]
              [:not-in
               :streams.id
               {:select [:stream_id]
                :from   [:playlist_streams]
                :where  [:not-in :playlist_id playlist-ids]}]]}))

(defn get-unique-streams-channels-for-non-ids
  [ds ids]
  (db/execute!
   ds
   {:select [:*]
    :from   [:streams]
    :where  [:and
             [:in :id ids]
             [:not-in :channel_id
              {:select [:channel_id]
               :from   [[:streams :st]]
               :where  [:>
                        {:select [:%count.*]
                         :from   [[:streams :s]]
                         :where  [:and [:= :s.channel_id :st.channel_id]
                                  [:not-in :st.id ids]]}
                        1]}]]}))

(defn add-channels
  [values ds]
  (db/execute! ds
               {:insert-into [:channels]
                :columns     [:url :name :avatar :verified]
                :values      values}))

(defn get-channel-by-url
  [url ds]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:channels]
                    :where  [:= :url url]}))

(defn delete-channels-by-ids
  [ds ids]
  (db/execute-one! ds
                   {:delete-from [:channels]
                    :where       [:in :id ids]}))
