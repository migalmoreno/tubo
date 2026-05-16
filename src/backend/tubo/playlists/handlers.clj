(ns tubo.playlists.handlers
  (:require
   [clojure.data :as data]
   [ring.util.http-response :refer [ok]]
   [tubo.middleware :as middleware]
   [tubo.queries :as queries]
   [tubo.playlists.queries :as playlist]
   [tubo.schemas :as s]
   [tubo.utils :as utils]))

(defn add-playlist-streams
  [{:keys [datasource] :as req} item playlist-id]
  (let [channel  (or (queries/get-channel-by-url (:uploader-url item)
                                                 datasource)
                     (first (queries/add-channels
                             [[(:uploader-url item)
                               (:uploader-name item)
                               (utils/unproxy-image (:uploader-avatar item) req)
                               (:uploader-verified item)]]
                             datasource)))
        stream   (or (queries/get-stream-by-url (:url item) datasource)
                     (first (queries/add-streams
                             [[(:duration item)
                               (:id channel)
                               (:name item)
                               (utils/unproxy-image (:thumbnail item) req)
                               (:url item)]]
                             datasource)))
        playlist (playlist/get-playlist-by-playlist-id playlist-id datasource)]
    (playlist/add-playlist-streams [[(:id stream) (:id playlist) (:order item)]]
                                   datasource)))

(defn create-get-auth-playlists-handler
  [{:keys [identity] :as req}]
  (ok (playlist/get-playlists-by-owner req (:id identity))))

(defn create-get-auth-playlist-handler
  [req]
  (ok (playlist/get-full-playlist-by-playlist-id req)))

(defn create-post-auth-playlists-handler
  [{:keys [datasource body-params identity] :as req}]
  (let [{:keys [playlist-id] :as added-playlist}
        (first (playlist/add-playlists [[(:name body-params)
                                         (:id identity)
                                         (:thumbnail body-params)]]
                                       datasource))]
    (when (seq (:items body-params))
      (doseq [item (map-indexed (fn [i item] (assoc item :order (inc i)))
                                (:items body-params))]
        (add-playlist-streams req item (str playlist-id))))
    (ok (assoc added-playlist
               :id    playlist-id
               :items (playlist/get-playlist-streams req (str playlist-id))))))

(defn create-delete-auth-playlists-handler
  [{:keys [identity] :as req}]
  (ok (playlist/delete-owner-playlists req (:id identity))))

(defn create-delete-auth-playlist-handler
  [{:keys [datasource path-params]}]
  (ok (playlist/delete-playlist-by-id datasource (:id path-params))))

(defn create-post-auth-playlist-add-streams-handler
  [{:keys [body-params path-params] :as req}]
  (let [playlist-streams (playlist/get-playlist-streams req (:id path-params))
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
        (add-playlist-streams req item (:id path-params))))
    (ok (into [] streams-to-add))))

(defn create-post-auth-playlist-delete-stream-handler
  [{:keys [datasource body-params path-params] :as req}]
  (let [playlist          (playlist/get-playlist-by-playlist-id
                           (:id path-params)
                           datasource)
        playlist-streams  (playlist/get-playlist-streams req (:id path-params))
        selected          (first (filter #(= (:url body-params)
                                             (:url %))
                                         playlist-streams))
        idx               (when selected
                            (.indexOf playlist-streams selected))
        unique-stream-ids (when selected
                            (->>
                              (queries/get-unique-streams-for-playlist
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

(def routes
  {:api/user-playlists
   {:get    {:summary    "returns all playlists for an authenticated user"
             :handler    create-get-auth-playlists-handler
             :middleware [middleware/auth]}
    :post   {:summary    "creates a new playlist for an authenticated user"
             :handler    create-post-auth-playlists-handler
             :middleware [middleware/auth]
             :parameters {:body s/UserPlaylist}}
    :delete {:summary    "deletes all playlists for an authenticated user"
             :handler    create-delete-auth-playlists-handler
             :middleware [middleware/auth]}}
   :api/user-playlist
   {:get    {:summary    "returns a user playlist for an authenticated user"
             :parameters {:path {:id string?}}
             :handler    create-get-auth-playlist-handler
             :middleware [middleware/auth]}
    :put    {:summary    "updates a user playlist for an authenticated user"
             :parameters {:path {:id string?}
                          :body s/UserPlaylist}
             :handler    create-update-auth-playlist-handler
             :middleware [middleware/auth]}
    :delete {:summary    "deletes a user playlist for an authenticated user"
             :parameters {:path {:id string?}}
             :handler    create-delete-auth-playlist-handler
             :middleware [middleware/auth]}}
   :api/add-user-playlist-streams
   {:post {:summary    "adds new playlist streams for a given user playlist"
           :handler    create-post-auth-playlist-add-streams-handler
           :middleware [middleware/auth]
           :parameters {:body [:vector s/UserPlaylistStream]}}}
   :api/delete-user-playlist-stream
   {:post {:summary    "deletes playlist stream for a given user playlist"
           :handler    create-post-auth-playlist-delete-stream-handler
           :middleware [middleware/auth]
           :parameters {:body s/UserPlaylistStream}}}})
