(ns tubo.utils
  (:require
   ["timeago.js" :as timeago]
   [clojure.string :as str]
   [clojure.data.xml :as xml]))

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
                                      str/upper-case)))
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

(defn titleize
  [str]
  (str/join " "
            (map #(if (not= % "and") (str/capitalize %) %)
                 (str/split str #"[\s_]+"))))

(defn ->Representation
  [format {:keys [mime-type]}]
  {:tag     :Representation
   :attrs   (merge {:id        (:itag format)
                    :codecs    (:codec format)
                    :bandwidth (:bitrate format)}
                   (when (str/includes? mime-type "video")
                     {:width          (:width format)
                      :height         (:height format)
                      :maxPlayoutRate "1"
                      :frameRate      (:fps format)}))
   :content (concat
             [{:tag :SegmentBase
               :attrs {:indexRange (str (:index-start format)
                                        "-"
                                        (:index-end format))}
               :content
               [{:tag   :Initialization
                 :attrs {:range (str (:init-start format)
                                     "-"
                                     (:init-end format))}}]}
              {:tag     :BaseURL
               :content [(str (:content format))]}]
             (when-not (str/includes? mime-type "video")
               [{:tag :AudioChannelConfiguration
                 :attrs
                 {:schemeIdUri
                  "urn:mpeg:dash:23003:3:audio_channel_configuration:2011"
                  :value "2"}}]))})

(defn ->AdaptationSet
  [{:keys [audio-track-id mime-type video-formats] :as item}]
  {:tag :AdaptationSet
   :attrs
   {:id                  audio-track-id
    :lang                (some-> audio-track-id
                                 (subs 0 2))
    :mimeType            mime-type
    :startsWithSAP       "1"
    :subsegmentAlignment "true"}
   :content (map #(->Representation % item) video-formats)})

(defn ->Period
  [{:keys [audio-streams video-streams video-only-streams]}]
  {:tag :Period
   :content
   (->> (concat audio-streams video-streams video-only-streams)
        (remove #(or (and (str/includes? (:mime-type %) "video")
                          (not (:video-only %)))
                     (str/includes? (:mime-type %) "application")))
        (reduce
         (fn [acc stream]
           (if (some #(and (= (:audio-track-id %) (:audio-track-id stream))
                           (= (:mime-type %) (:mime-type stream)))
                     acc)
             (map #(if (= (:audio-track-id %) (:audio-track-id stream))
                     (update % :video-formats conj stream)
                     %)
                  acc)
             (conj acc (assoc stream :video-formats [stream]))))
         [])
        (map ->AdaptationSet))})

(defn ->mpd-file
  [{:keys [duration] :as stream}]
  (xml/emit-str
   {:tag     (xml/qname "urn:mpeg:dash:schema:mpd:2011" "MPD")
    :attrs   {:xmlns/xsi "http://www.w3.org/2001/XMLSchema-instance"
              :type "static"
              :mediaPresentationDuration (str "PT" duration "S")
              :minBufferTime "PT1.5S"
              :profiles "urn:mpeg:dash:profile:full:2011"}
    :content [(->Period stream)]}))
