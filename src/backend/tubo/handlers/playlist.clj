(ns tubo.handlers.playlist
  (:require
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.playlist.PlaylistInfo))

(defn create-playlist-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params :as req}]
  (let [url* (url-decode url)
        info (if nextPage
               (PlaylistInfo/getMoreItems (NewPipe/getServiceByUrl url*)
                                          url*
                                          (utils/create-page nextPage))
               (PlaylistInfo/getInfo (url-decode url)))]
    (when info
      (ok (utils/->ListInfo info req)))))
