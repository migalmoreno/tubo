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
  [url]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (PlaylistInfo/getInfo service (url-decode url))]
    {:name             (.getName info)
     :service-id       (.getServiceId info)
     :related-streams  (utils/get-items (.getRelatedItems info))
     :id               (.getId info)
     :playlist-type    (j/from-java (.getPlaylistType info))
     :thumbnails       (j/from-java (.getThumbnails info))
     :banners          (j/from-java (.getBanners info))
     :uploader-name    (.getUploaderName info)
     :uploader-url     (.getUploaderUrl info)
     :uploader-avatars (j/from-java (.getUploaderAvatars info))
     :stream-count     (.getStreamCount info)
     :next-page        (utils/get-next-page info)}))

(defn get-playlist-page
  [url next-page]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (PlaylistInfo/getMoreItems service
                                           url
                                           (utils/create-page next-page))]
    {:next-page       (utils/get-next-page info)
     :related-streams (utils/get-items (.getItems info))}))

(defn create-playlist-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (ok (if nextPage (get-playlist-page url nextPage) (get-playlist url))))
