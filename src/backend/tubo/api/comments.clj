(ns tubo.api.comments
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.ListExtractor
   org.schabi.newpipe.extractor.comments.CommentsInfoItem
   org.schabi.newpipe.extractor.comments.CommentsInfo))

(defn get-comment-item
  [item extractor]
  {:id (.getCommentId item)
   :text (.. item (getCommentText) (getContent))
   :uploader-name (.getUploaderName item)
   :uploader-avatar (.getUploaderAvatarUrl item)
   :uploader-url (.getUploaderUrl item)
   :uploader-verified? (.isUploaderVerified item)
   :upload-date (.getTextualUploadDate item)
   :like-count (when-not (= (.getLikeCount item) -1) (.getLikeCount item))
   :reply-count (when-not (= (.getReplyCount item) -1) (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned? (.isPinned item)
   :stream-position (when-not (= (.getStreamPosition item) -1) (.getStreamPosition item))
   :replies (when (.getReplies item)
              (if extractor
                (let [comments-page (.getPage extractor (.getReplies item))]
                  {:next-page (when (.hasNextPage comments-page) (j/from-java (.getNextPage comments-page)))
                   :items (map #(get-comment-item % extractor) (.getItems comments-page))})
                (j/from-java (.getReplies item))))})

(defn get-comments
  ([url]
   (let [info (CommentsInfo/getInfo (url-decode url))
         extractor (.getCommentsExtractor info)]
     {:comments (map #(get-comment-item % extractor) (.getRelatedItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? (.isCommentsDisabled info)}))
  ([url page-url]
   (let [service (NewPipe/getServiceByUrl (url-decode url))
         info (CommentsInfo/getMoreItems service (url-decode url) (Page. (url-decode page-url)))]
     {:comments (map #(get-comment-item % nil) (.getItems info))
      :next-page (j/from-java (.getNextPage info))
      :disabled? false})))
