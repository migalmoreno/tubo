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
  [item req]
  {:id                   (.getCommentId item)
   :text                 (.. item (getCommentText) (getContent))
   :upload-date          (.getTextualUploadDate item)
   :uploader-name        (.getUploaderName item)
   :uploader-url         (.getUploaderUrl item)
   :uploader-avatars     (utils/proxy-images (.getUploaderAvatars item) req)
   :uploader-verified?   (.isUploaderVerified item)
   :creator-reply?       (.hasCreatorReply item)
   :channel-owner?       (.isChannelOwner item)
   :like-count           (utils/non-negative (.getLikeCount item))
   :reply-count          (utils/non-negative (.getReplyCount item))
   :hearted-by-uploader? (.isHeartedByUploader item)
   :pinned?              (.isPinned item)
   :stream-position      (utils/non-negative (.getStreamPosition item))
   :replies-page         (j/from-java (.getReplies item))})

(defn get-comments
  [{{:keys [url]} :path-params :as req}]
  (when-let [info (CommentsInfo/getInfo (url-decode url))]
    {:comments       (map #(get-comment-item % req) (.getRelatedItems info))
     :comments-count (.getCommentsCount info)
     :next-page      (utils/get-next-page info)
     :disabled?      (.isCommentsDisabled info)}))

(defn get-comments-page
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params :as req}]
  (let [service (NewPipe/getServiceByUrl (url-decode url))
        info    (CommentsInfo/getMoreItems service
                                           (url-decode url)
                                           (utils/create-page nextPage))]
    {:comments  (map #(get-comment-item % req) (.getItems info))
     :next-page (utils/get-next-page info)
     :disabled? false}))

(defn create-comments-handler
  [{{:strs [nextPage]} :query-params :as req}]
  (ok (if nextPage (get-comments-page req) (get-comments req))))
