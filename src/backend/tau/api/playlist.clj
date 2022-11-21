(ns tau.api.playlist
  (:require
   [clojure.java.data :as j]
   [tau.api.stream :as stream]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.playlist.PlaylistInfo
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.NewPipe))

(defrecord Playlist
    [id name playlist-type thumbnail-url uploader-name uploader-url
     uploader-avatar banner-url next-page stream-count related-streams])

(defrecord PlaylistResult
    [name thumbnail-url url upload-author stream-count])

(defrecord PlaylistPage
    [next-page related-streams])

(defn get-playlist-result
  [playlist]
  (map->PlaylistResult
   {:name (.getName playlist)
    :thumbnail-url (.getThumbnailUrl playlist)
    :url (.getUrl playlist)
    :upload-author (.getUploaderName playlist)
    :stream-count (.getStreamCount playlist)}))

(defn get-playlist-info
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
       :related-streams (map #(stream/get-stream-result %) (.getRelatedItems info))})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (PlaylistInfo/getMoreItems service url (Page. (url-decode page-url)))]
     (map->PlaylistPage
      {:next-page (j/from-java (.getNextPage info))
       :related-streams (map #(stream/get-stream-result %) (.getItems info))}))))
