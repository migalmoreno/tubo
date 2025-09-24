(ns tubo.handlers.playlist
  (:require
   [clojure.java.data :as j]
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.playlist.PlaylistInfo))

(defn get-playlist
  [{{:keys [url]} :path-params :as req}]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (PlaylistInfo/getInfo service (url-decode url))]
    {:name             (.getName info)
     :service-id       (.getServiceId info)
     :related-streams  (utils/get-items (.getRelatedItems info) req)
     :id               (.getId info)
     :playlist-type    (j/from-java (.getPlaylistType info))
     :thumbnails       (utils/proxy-images (.getThumbnails info) req)
     :banners          (utils/proxy-images (.getBanners info) req)
     :uploader-name    (.getUploaderName info)
     :uploader-url     (.getUploaderUrl info)
     :uploader-avatars (utils/proxy-images (.getUploaderAvatars info) req)
     :stream-count     (utils/non-negative (.getStreamCount info))
     :next-page        (utils/get-next-page info)}))

(defn get-playlist-page
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params :as req}]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (PlaylistInfo/getMoreItems service
                                           url
                                           (utils/create-page nextPage))]
    {:next-page       (utils/get-next-page info)
     :related-streams (utils/get-items (.getItems info) req)}))

(defn create-playlist-handler
  [{{:strs [nextPage]} :query-params :as req}]
  (ok (if nextPage (get-playlist-page req) (get-playlist req))))
