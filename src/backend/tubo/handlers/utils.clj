(ns tubo.handlers.utils
  (:require
   [clojure.data.json :as json]
   [clojure.java.data :as j]
   [clojure.string :as str]
   [ring.util.codec :refer [url-decode url-encode]])
  (:import
   java.net.URL
   org.schabi.newpipe.extractor.Page))

(defn non-negative
  [val]
  (when (pos-int? val) val))

(defn get-next-page
  [info]
  (when (.hasNextPage info)
    (update (j/from-java (.getNextPage info))
            :body
            #(slurp (byte-array %)))))

(defn create-page
  [next-page]
  (let [page (json/read-str next-page)]
    (Page. (get page "url")
           (get page "id")
           (get page "ids")
           (get page "cookies")
           (when-let [body (get page "body")] (.getBytes body)))))

(defn proxy-image
  [image req]
  (when (seq image)
    (if (str/includes? (.getHost (URL. image)) "sndcdn.com")
      image
      (str (name (:scheme req))
           "://" (:server-name req)
           ":" (:server-port req)
           "/proxy/"
           (url-encode image)))))

(defn proxy-images
  [images req]
  (map #(assoc % :url (proxy-image (:url %) req)) (j/from-java images)))

(defn unproxy-image
  [image req]
  (let [proxy-url (str (name (:scheme req))
                       "://"
                       (:server-name req)
                       ":"
                       (:server-port req)
                       "/proxy/")]
    (when (seq image)
      (if (str/includes? image proxy-url)
        (-> (URL. image)
            (.getPath)
            (str/split #"/proxy/")
            last
            url-decode)
        image))))

(defn get-stream-item
  [stream req]
  {:type               :stream
   :service-id         (.getServiceId stream)
   :url                (.getUrl stream)
   :name               (.getName stream)
   :thumbnails         (proxy-images (.getThumbnails stream) req)
   :short?             (.isShortFormContent stream)
   :uploader-name      (.getUploaderName stream)
   :uploader-url       (.getUploaderUrl stream)
   :uploader-avatars   (proxy-images (.getUploaderAvatars stream) req)
   :upload-date        (.getTextualUploadDate stream)
   :description        (.getShortDescription stream)
   :duration           (non-negative (.getDuration stream))
   :view-count         (non-negative (.getViewCount stream))
   :stream-type        (.getStreamType stream)
   :uploaded           (when (.getUploadDate stream)
                         (.. stream
                             (getUploadDate)
                             (offsetDateTime)
                             (toInstant)
                             (toEpochMilli)))
   :uploader-verified? (.isUploaderVerified stream)})

(defn get-channel-item
  [channel req]
  {:type             :channel
   :service-id       (.getServiceId channel)
   :url              (.getUrl channel)
   :name             (.getName channel)
   :thumbnails       (proxy-images (.getThumbnails channel) req)
   :description      (.getDescription channel)
   :subscriber-count (non-negative (.getSubscriberCount channel))
   :stream-count     (non-negative (.getStreamCount channel))
   :verified?        (.isVerified channel)})

(defn get-playlist-item
  [playlist req]
  {:type          :playlist
   :service-id    (.getServiceId playlist)
   :url           (.getUrl playlist)
   :name          (.getName playlist)
   :thumbnails    (proxy-images (.getThumbnails playlist) req)
   :uploader-name (.getUploaderName playlist)
   :uploader-url  (.getUploaderUrl playlist)
   :playlist-type (.getPlaylistType playlist)
   :stream-count  (non-negative (.getStreamCount playlist))})

(defn get-items
  [items req]
  (map #(case (.name (.getInfoType %))
          "STREAM"   (get-stream-item % req)
          "CHANNEL"  (get-channel-item % req)
          "PLAYLIST" (get-playlist-item % req))
       items))
