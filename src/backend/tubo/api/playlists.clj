(ns tubo.api.playlists
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [tubo.api.items :as items])
  (:import
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.NewPipe))

(defn get-playlist
  ([url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (PlaylistInfo/getInfo service (url-decode url))]
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
      :related-streams (items/get-items (.getRelatedItems info))
      :service-id (.getServiceId info)}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (PlaylistInfo/getMoreItems service url (Page. (url-decode page-url)))]
     {:next-page (j/from-java (.getNextPage info))
      :related-streams (items/get-items (.getItems info))})))
