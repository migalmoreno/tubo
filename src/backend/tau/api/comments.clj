(ns tau.api.comments
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.comments.CommentsInfoItem
   org.schabi.newpipe.extractor.comments.CommentsInfo))

(defn get-comment-item
  [item]
  {:id (.getCommentId item)
   :text (.getCommentText item)
   :uploader-name (.getUploaderName item)
   :uploader-avatar (.getUploaderAvatarUrl item)
   :uploader-url (.getUploaderUrl item)
   :uploader-verified? (.isUploaderVerified item)
   :upload-date (.getTextualUploadDate item)      
   :like-count (when-not (= (.getLikeCount item) -1) (.getLikeCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned? (.isPinned item)
   :replies (when (.getReplies item)
              (j/from-java (.getReplies item)))})

(defn get-comment
  ([url]
   (let [info (CommentsInfo/getInfo (url-decode url))]
     {:comments (map get-comment-item (.getRelatedItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? (.isCommentsDisabled info)}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (CommentsInfo/getMoreItems service url (Page. (url-decode page-url)))]
     {:comments (map get-comment-item (.getItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? false})))
