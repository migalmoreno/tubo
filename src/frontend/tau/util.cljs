(ns tau.util)

(defn format-quantity
  [num]
  (.format
   (js/Intl.NumberFormat
    "en-US" #js {"notation" "compact" "maximumFractionDigits" 1})
   num))

(defn format-duration
  [num]
  (let [duration (js/Date. (* num 1000))
        slice (if (> (.getHours duration) 1)
                #(.slice % 11 19)
                #(.slice % 14 19))]
    (-> duration (.toISOString) slice)))
