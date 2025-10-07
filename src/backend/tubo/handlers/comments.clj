(ns tubo.handlers.comments
  (:require
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.NewPipe))

(defn create-comments-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params :as req}]
  (let [url* (url-decode url)
        info (if nextPage
               (CommentsInfo/getMoreItems (NewPipe/getServiceByUrl url*)
                                          url*
                                          (utils/create-page nextPage))
               (CommentsInfo/getInfo url*))]
    (when info
      (ok (utils/->ListInfo info req)))))
