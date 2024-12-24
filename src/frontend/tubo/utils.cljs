(ns tubo.utils
  (:require
   ["timeago.js" :as timeago]))

(goog-define ^js/String version "unknown")

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
