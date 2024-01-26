(ns tubo.util
  (:require
   ["timeago.js" :as timeago]))

(defn format-date
  [date]
  (if (-> date js/Date.parse js/isNaN)
    date
    (timeago/format date)))

(defn format-quantity
  [num]
  (.format
   (js/Intl.NumberFormat
    "en-US" #js {"notation" "compact" "maximumFractionDigits" 1})
   num))

(defn format-duration
  [num]
  (let [duration (js/Date. (* num 1000))
        slice #(.slice % (if (>= (.getUTCHours duration) 1) 11 14) 19)]
    (-> duration (.toISOString) slice)))
