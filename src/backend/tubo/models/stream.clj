(ns tubo.models.stream
  (:require
   [tubo.db :as db]))

(defn add-stream
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

(defn get-streams-by-ids
  [ds ids]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:streams]
                    :where  [:in :id ids]}))

(defn get-streams-by-channel-id
  [ds id]
  (db/execute! ds
               {:select [:*]
                :from   [:streams]
                :where  [:= :channel-id id]}))

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

(defn get-all-unique-streams-for-playlist
  [ds id]
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
              [:= :playlists.id id]
              [:not-in
               :streams.id
               {:select [:stream_id]
                :from   [:playlist_streams]
                :where  [:<> :playlist_id id]}]]}))

(defn get-unique-streams-channels
  [ds ids]
  (db/execute!
   ds
   {:select [:*]
    :from   [:streams]
    :where  [:and
             [:not-in :channel_id
              {:select [:channel_id]
               :from   [[:streams :st]]
               :where  [:>
                        {:select [:%count.*]
                         :from   [[:streams :s]]
                         :where  [:= :s.channel_id :st.channel_id]}
                        1]}]
             [:in :id ids]]}))
