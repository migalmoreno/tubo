(ns tubo.handlers.utils
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.data.json :as json]
   [clojure.java.data :as j]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]
   [ring.util.codec :refer [url-decode url-encode]])
  (:import
   java.net.URL
   org.schabi.newpipe.extractor.Page))

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

(defn ->Info
  [info]
  (cske/transform-keys
   csk/->kebab-case-keyword
   (j/from-java-deep info {:exceptions :omit :omit #{:service :errors}})))

(defn ->RelatedItem
  [{:keys [info-type] :as item} req item*]
  (cond-> item
    (= info-type "COMMENT")  (-> (rename-keys {:replies :replies-page})
                                 (update :stream-position
                                         #(when (pos-int? %) %))
                                 (update :reply-count #(when (pos-int? %) %))
                                 (update :like-count #(when (pos-int? %) %)))
    (= info-type "STREAM")   (-> (update :view-count #(when (pos-int? %) %))
                                 (update :duration #(when (pos-int? %) %))
                                 (assoc :upload-date
                                        (when (.getUploadDate item*)
                                          (.. item*
                                              (getUploadDate)
                                              (offsetDateTime)
                                              (toInstant)
                                              (toEpochMilli)))))
    (= info-type "CHANNEL")  (-> (update :subscriber-count
                                         #(when (pos-int? %) %))
                                 (update :stream-count #(when (pos-int? %) %)))
    (= info-type "PLAYLIST") (update :stream-count #(when (pos-int? %) %))
    :else                    (-> (update :thumbnails #(proxy-images % req))
                                 (update :uploader-avatars
                                         #(proxy-images % req)))))

(defn ->RelatedItems
  [items req items*]
  (map-indexed (fn [idx item]
                 (->RelatedItem item req (.get items* idx)))
               items))

(defn ->ListInfo
  [info req]
  (let [info* (->Info info)]
    (cond-> info*
      (:next-page info*)     (assoc :next-page (get-next-page info))
      (:related-items info*) (update
                              :related-items
                              #(->RelatedItems % req (.getRelatedItems info)))
      (:items info*)         (update
                              :items
                              #(->RelatedItems % req (.getItems info))))))
