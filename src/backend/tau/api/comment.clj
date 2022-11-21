(ns tau.api.comment
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.comments.CommentsInfoItem
   org.schabi.newpipe.extractor.comments.CommentsInfo))

(defrecord CommentsPage
    [next-page disabled? comments])

(defrecord Comment
    [id text upload-name upload-avatar upload-date upload-url
     upload-verified? like-count hearted-by-upload? pinned? replies])

(defn get-comment-result
  [comment]
  (map->Comment
   {:id (.getCommentId comment)
    :text (.getCommentText comment)
    :upload-name (.getUploaderName comment)
    :upload-avatar (.getUploaderAvatarUrl comment)
    :upload-date (.getTextualUploadDate comment)
    :upload-url (.getUploaderUrl comment)
    :upload-verified? (.isUploaderVerified comment)
    :like-count (.getLikeCount comment)
    :hearted-by-upload? (.isHeartedByUploader comment)
    :pinned? (.isPinned comment)
    :replies (when (.getReplies comment)
               (j/from-java (.getReplies comment)))}))

(defn get-comments-info
  ([url]
   (let [info (CommentsInfo/getInfo (url-decode url))]
     (map->CommentsPage
      {:comments (map #(get-comment-result %) (.getRelatedItems info))
       :next-page (j/from-java (.getNextPage info))
       :disabled? (.isCommentsDisabled info)})))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (CommentsInfo/getMoreItems service url (Page. (url-decode page-url)))]
     (map->CommentsPage
      {:comments (map #(get-comment-result %) (.getItems info))
       :next-page (j/from-java (.getNextPage info))
       :disabled? false}))))
