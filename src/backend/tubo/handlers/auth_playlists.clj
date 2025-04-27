(ns tubo.handlers.auth-playlists
  (:require
   [clojure.data :as data]
   [ring.util.http-response :refer [ok bad-request]]
   [tubo.models.playlist :as playlist]
   [tubo.models.channel :as channel]
   [tubo.models.stream :as stream]))

(defn create-get-auth-playlists-handler
  [{:keys [datasource identity]}]
  (let [playlists (playlist/get-playlists-by-owner (:id identity) datasource)]
    (if (seq playlists)
      (ok playlists)
      (ok (playlist/add-playlists [["Liked Streams" (:id identity)]]
                                  datasource)))))

(defn create-get-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/get-playlist-by-playlist-id (:id path-params) datasource)))

(defn create-post-auth-playlists-handler
  [{:keys [datasource body-params identity]}]
  (let [added-playlist (first (playlist/add-playlists [[(:name body-params)
                                                        (:id identity)]]
                                                      datasource))]
    (ok (assoc added-playlist :id (:playlist-id added-playlist)))))

(defn create-delete-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/delete-playlist (:id path-params) datasource)))

(defn create-post-auth-playlist-handler
  [{:keys [datasource body-params path-params]}]
  (let [channel  (or (channel/get-channel-by-url (:uploader-url body-params)
                                                 datasource)
                     (first (channel/add-channel
                             [(into []
                                    (map body-params
                                         [:uploader-url
                                          :uploader-name
                                          :uploader-avatar
                                          :uploader-verified?]))]
                             datasource)))
        stream   (or (stream/get-stream-by-url (:url body-params)
                                               datasource)
                     (first (stream/add-stream
                             [(into (map body-params [:name :thumbnail :url])
                                    [(:id channel) (:duration body-params)])]
                             datasource)))
        playlist (playlist/get-playlist-by-playlist-id (:id path-params)
                                                       datasource)]
    (try
      (some->
        (first (playlist/add-playlist-streams [[(:id stream)
                                                (:id
                                                 playlist)]]
                                              datasource))
        :playlist-id
        (playlist/get-playlist-by-id datasource)
        ok)
      (catch Exception _
        (bad-request "There was a problem adding stream to playlist")))))

(defn create-update-auth-playlist-handler
  [{:keys [datasource body-params path-params]}]
  (let [streams              (playlist/get-playlist-streams (:id path-params)
                                                            datasource)
        diff-streams         (data/diff
                              (into #{} (map :id streams))
                              (into #{} (map :id (:items body-params))))
        stream-ids-to-delete (first diff-streams)
        unique-stream-ids    (->> (stream/get-unique-streams-for-playlist
                                   datasource
                                   (:id body-params)
                                   stream-ids-to-delete)
                                  (map :stream-id))]
    (when (seq stream-ids-to-delete)
      (playlist/delete-playlist-streams-by-ids (:id body-params)
                                               stream-ids-to-delete
                                               datasource))
    (when (seq unique-stream-ids)
      (let [unique-channel-ids
            (map :channel-id
                 (stream/get-unique-streams-channels datasource
                                                     unique-stream-ids))]
        (stream/delete-streams-by-ids datasource unique-stream-ids)
        (channel/delete-channels-by-ids datasource unique-channel-ids)))
    (ok (assoc (playlist/update-playlist (:id path-params)
                                         (select-keys body-params
                                                      [:name :thumbnail])
                                         datasource)
               :items
               (:items body-params)))))
