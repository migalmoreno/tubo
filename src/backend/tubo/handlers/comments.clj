(ns tubo.handlers.comments
  (:require
   [clojure.java.data :as j]
   [ring.util.http-response :refer [ok]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.comments.CommentsInfo
   org.schabi.newpipe.extractor.NewPipe))

(defn get-comment-item
  [item extractor info]
  {:id                   (.getCommentId item)
   :text                 (.. item (getCommentText) (getContent))
   :upload-date          (.getTextualUploadDate item)
   :uploader-name        (.getUploaderName item)
   :uploader-url         (.getUploaderUrl item)
   :uploader-avatars     (j/from-java (.getUploaderAvatars item))
   :uploader-verified?   (.isUploaderVerified item)
   :creator-reply?       (.hasCreatorReply item)
   :channel-owner?       (.isChannelOwner item)
   :like-count           (utils/non-negative (.getLikeCount item))
   :reply-count          (utils/non-negative (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned?              (.isPinned item)
   :stream-position      (utils/non-negative (.getStreamPosition item))
   :replies              (when (.getReplies item)
                           (if extractor
                             (let [comments-page (.getPage extractor
                                                           (.getReplies item))]
                               {:next-page (utils/get-next-page comments-page)
                                :items     (map #(get-comment-item %
                                                                   extractor
                                                                   info)
                                                (.getItems comments-page))})
                             (j/from-java (.getReplies item))))})

(defn get-comments
  [url]
  (let [info      (CommentsInfo/getInfo (url-decode url))
        extractor (.getCommentsExtractor info)]
    {:comments  (map #(get-comment-item % extractor info)
                     (.getRelatedItems info))
     :next-page (utils/get-next-page info)
     :disabled? (.isCommentsDisabled info)}))

(defn get-comments-page
  [url next-page]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (CommentsInfo/getMoreItems service
                                           (url-decode url)
                                           (utils/create-page next-page))]
    {:comments  (map #(get-comment-item % nil info) (.getItems info))
     :next-page (utils/get-next-page info)
     :disabled? false}))

(defn create-comments-handler
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (ok (if nextPage (get-comments-page url nextPage) (get-comments url))))
