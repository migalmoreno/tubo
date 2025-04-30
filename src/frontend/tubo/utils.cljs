(ns tubo.utils
  (:require
   ["timeago.js" :as timeago]
   [clojure.string :as s]))

(goog-define ^js/String version "unknown")

(defn apply-image-quality
  [body db new-image-key old-image-key]
  (assoc
   body
   new-image-key
   (or (get body new-image-key)
       (when (and (not= (get-in db [:settings :image-quality]) "none")
                  (seq (get body old-image-key)))
         (or (some-> (filter (fn [t]
                               (= (:estimatedResolutionLevel t)
                                  (-> db
                                      (get-in [:settings
                                               :image-quality])
                                      str
                                      s/upper-case)))
                             (get body old-image-key))
                     first
                     :url)
             (-> (get body old-image-key)
                 last
                 :url)
             (get body old-image-key))))))

(defn apply-image-quality-n
  [body db key new-image-key old-image-key]
  (update body
          key
          #(into []
                 (map (fn [n]
                        (apply-image-quality n db new-image-key old-image-key))
                      %))))

(defn apply-thumbnails-quality
  [body db key]
  (apply-image-quality-n body db key :thumbnail :thumbnails))

(defn apply-avatars-quality
  [body db key]
  (apply-image-quality-n body db key :uploader-avatar :uploader-avatars))

(defn get-service-color
  [id]
  (when id
    (case id
      0 "#cc0000"
      1 "#ff7700"
      2 "#333333"
      3 "#F2690D"
      4 "#629aa9")))

(defn get-service-name
  [id]
  (when id
    (case id
      0 "YouTube"
      1 "SoundCloud"
      2 "media.ccc.de"
      3 "PeerTube"
      4 "Bandcamp")))

(defn format-date-string
  [date]
  (-> date
      js/Date.parse
      js/Date.
      .toDateString))

(defn format-date-ago
  [date]
  (if (-> date
          js/Date.parse
          js/isNaN)
    date
    (timeago/format date)))

(defn format-quantity
  [num]
  (.format
   (js/Intl.NumberFormat
    "en-US"
    #js {"notation" "compact" "maximumFractionDigits" 1})
   num))

(defn format-duration
  [num]
  (let [duration (and (not (js/isNaN num)) (js/Date. (* num 1000)))
        slice    (and duration
                      #(.slice % (if (>= (.getUTCHours duration) 1) 11 14) 19))]
    (if slice
      (-> duration
          (.toISOString)
          slice)
      "--:--")))
