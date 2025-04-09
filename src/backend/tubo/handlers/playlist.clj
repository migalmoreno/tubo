(ns tubo.handlers.playlist
  (:require
   [clojure.java.data :as j]
   [ring.util.response :refer [response]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :refer [get-items]])
  (:import
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defn get-playlist
  ([url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info    (PlaylistInfo/getInfo service (url-decode url))]
     {:name             (.getName info)
      :service-id       (.getServiceId info)
      :related-streams  (get-items (.getRelatedItems info))
      :id               (.getId info)
      :playlist-type    (j/from-java (.getPlaylistType info))
      :thumbnails       (j/from-java (.getThumbnails info))
      :banners          (j/from-java (.getBanners info))
      :uploader-name    (.getUploaderName info)
      :uploader-url     (.getUploaderUrl info)
      :uploader-avatars (j/from-java (.getUploaderAvatars info))
      :stream-count     (.getStreamCount info)
      :next-page        (j/from-java (.getNextPage info))}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info
         (PlaylistInfo/getMoreItems service url (Page. (url-decode page-url)))]
     {:next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getItems info))})))

(defn create-playlist-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply get-playlist url (if nextPage [nextPage] []))))
