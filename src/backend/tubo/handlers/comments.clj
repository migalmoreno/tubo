(ns tubo.handlers.comments
  (:require
   [clojure.java.data :refer [from-java]]
   [ring.util.response :refer [response]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :refer [non-negative]])
  (:import
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defn get-comment-item
  [item extractor info]
  {:id                   (.getCommentId item)
   :text                 (.. item (getCommentText) (getContent))
   :upload-date          (.getTextualUploadDate item)
   :uploader-name        (.getUploaderName item)
   :uploader-url         (.getUploaderUrl item)
   :uploader-avatars     (from-java (.getUploaderAvatars item))
   :uploader-verified?   (.isUploaderVerified item)
   :creator-reply?       (.hasCreatorReply item)
   :channel-owner?       (.isChannelOwner item)
   :like-count           (non-negative (.getLikeCount item))
   :reply-count          (non-negative (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned?              (.isPinned item)
   :stream-position      (non-negative (.getStreamPosition item))
   :replies              (when (.getReplies item)
                           (if extractor
                             (let [comments-page (.getPage extractor
                                                           (.getReplies item))]
                               {:next-page (when (.hasNextPage comments-page)
                                             (from-java (.getNextPage
                                                         comments-page)))
                                :items     (map #(get-comment-item %
                                                                   extractor
                                                                   info)
                                                (.getItems comments-page))})
                             (from-java (.getReplies item))))})

(defn get-comments
  ([url]
   (let [info      (CommentsInfo/getInfo (url-decode url))
         extractor (.getCommentsExtractor info)]
     {:comments  (map #(get-comment-item % extractor info)
                      (.getRelatedItems info))
      :next-page (from-java (.getNextPage info))
      :disabled? (.isCommentsDisabled info)}))
  ([url page-url]
   (let [service       (NewPipe/getServiceByUrl (url-decode url))
         comments-info (CommentsInfo/getInfo (url-decode url))
         next-page     (.getNextPage comments-info)
         info          (CommentsInfo/getMoreItems service
                                                  (url-decode url)
                                                  (Page. (url-decode page-url)
                                                         (.getId next-page)
                                                         (.getIds next-page)
                                                         (.getCookies next-page)
                                                         (.getBody next-page)))]
     {:comments  (map #(get-comment-item % nil info) (.getItems info))
      :next-page (from-java (.getNextPage info))
      :disabled? false})))

(defn create-comments-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply get-comments url (if nextPage [nextPage] []))))
