(ns tubo.player.utils
  (:require
   [clojure.string :as s]
   [tubo.utils :as utils]))

(defn fmt=
  [format str]
  (s/includes? (s/replace (:format format) #"[-_]" "")
               (s/replace (s/upper-case str) #"[-_]" "")))

(defn get-video-stream
  [{:keys [audio-streams video-streams service-id] :as stream} settings]
  (cond
    (and (= (:video-source-type settings) "dash") (= service-id 0))
    (str "data:application/dash+xml;charset=utf-8;base64,"
         (js/btoa (utils/->mpd-file stream)))
    :else
    (or (some->> video-streams
                 (some #(when (fmt= % (:default-video-format settings)) %))
                 (some #(when (= (:resolution %) (:default-resolution settings))
                          %))
                 :content)
        (some-> (if (= (:video-source-type settings)
                       "progressive-http")
                  (remove #(= (:delivery-method %) "HLS") video-streams)
                  video-streams)
                first
                :content)
        (some->> (if (= (:video-source-type settings) "progressive-http")
                   (remove #(= (:delivery-method %) "HLS")
                           audio-streams)
                   audio-streams)
                 (some #(when (fmt= % (:default-audio-format settings))
                          %))
                 :content)
        (some-> (if (= (:video-source-type settings)
                       "progressive-http")
                  (remove #(= (:delivery-method %) "HLS") audio-streams)
                  audio-streams)
                first
                :content))))

(defn get-audio-stream
  [{:keys [audio-streams video-streams]} settings]
  (or (some->> audio-streams
               (remove #(= (:delivery-method %) "HLS"))
               (some #(when (fmt= % (:default-audio-format settings)) %))
               :content)
      (some->> audio-streams
               (remove #(= (:delivery-method %) "HLS"))
               first
               :content)
      (some->> video-streams
               (some #(when (fmt= % (:default-video-format settings)) %))
               (some #(when (= (:resolution %) (:default-resolution settings))
                        %))
               :content)
      (:content (first audio-streams))))
