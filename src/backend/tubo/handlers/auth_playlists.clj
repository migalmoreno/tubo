(ns tubo.handlers.auth-playlists
  (:require
   [ring.util.http-response :refer [ok]]
   [ring.util.response :refer [response]]
   [tubo.models.playlist :as playlist]
   [tubo.models.channel :as channel]
   [tubo.models.stream :as stream]))

(defn create-get-auth-playlists-handler
  [{:keys [datasource identity]}]
  (let [playlists (playlist/get-playlists-by-owner (:id identity) datasource)]
    (if (seq playlists)
      (response playlists)
      (response (playlist/add-playlists [["Liked Streams" (:id identity)]]
                                        datasource)))))

(defn create-get-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (let [streams        (playlist/get-playlist-streams (:id path-params)
                                                      datasource)
        added-playlist (playlist/get-playlist-by-playlist-id (:id path-params)
                                                             datasource)]
    (response (assoc added-playlist
                     :id (:playlist-id added-playlist)
                     :items
                     streams))))

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
                     (channel/add-channel [(into []
                                                 (map body-params
                                                      [:uploader-url
                                                       :uploader-name
                                                       :uploader-avatar
                                                       :uploader-verified?]))]
                                          datasource))
        stream   (or (stream/get-stream-by-url (:url body-params) datasource)
                     (stream/add-stream
                      [(into (map body-params [:name :thumbnail :url])
                             [(:url channel)
                              (:duration body-params)])]
                      datasource))
        playlist (playlist/get-playlist-by-playlist-id (:id path-params)
                                                       datasource)]
    (println "new stream" stream)
    (when (and stream playlist)
      (playlist/add-playlist-streams [[(:id stream) (:id playlist)]]
                                     datasource))
    (when playlist
      (ok playlist))))

(defn create-update-auth-playlist-handler
  [{:keys [datasource body-params path-params]}]
  (playlist/update-playlist (:id path-params) body-params datasource))
