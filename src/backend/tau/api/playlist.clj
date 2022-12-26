(ns tau.api.playlist
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tau.api.result :as result])
  (:import
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.NewPipe))

(defrecord Playlist
    [id name playlist-type thumbnail-url uploader-name uploader-url
     uploader-avatar banner-url next-page stream-count related-streams])

(defrecord PlaylistPage
    [next-page related-streams])

(defn get-info
  ([url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (PlaylistInfo/getInfo service (url-decode url))]
     (map->Playlist
      {:id (.getId info)
       :name (.getName info)
       :playlist-type (j/from-java (.getPlaylistType info))
       :thumbnail-url (.getThumbnailUrl info)
       :banner-url (.getBannerUrl info)
       :uploader-name (.getUploaderName info)
       :uploader-url (.getUploaderUrl info)
       :uploader-avatar (.getUploaderAvatarUrl info)
       :stream-count (.getStreamCount info)
       :next-page (j/from-java (.getNextPage info))
       :related-streams (result/get-results (.getRelatedItems info))})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (PlaylistInfo/getMoreItems service url (Page. (url-decode page-url)))]
     (map->PlaylistPage
      {:next-page (j/from-java (.getNextPage info))
       :related-streams (result/get-results (.getItems info))}))))
