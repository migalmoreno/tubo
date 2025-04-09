(ns tubo.player.utils
  (:require
   [clojure.string :as s]))

(defn fmt=
  [format str]
  (s/includes? (s/replace (:format format) #"[-_]" "")
               (s/replace (s/upper-case str) #"[-_]" "")))

(defn get-video-stream
  [{:keys [audio-streams video-streams]} settings]
  (-> (cond (seq video-streams)
            (as-> video-streams streams
              (if (some #(fmt= % (:default-video-format settings)) streams)
                (filter #(fmt= % (:default-video-format settings))
                        streams)
                streams)
              (if (some #(= (:resolution %)
                            (:default-resolution settings))
                        video-streams)
                (filter #(= (:resolution %)
                            (:default-resolution settings))
                        streams)
                streams)
              (first streams))
            (seq audio-streams)
            (as-> audio-streams streams
              (remove #(= (:deliveryMethod %) "HLS") streams)
              (if (some #(fmt= % (:default-audio-format settings))
                        streams)
                (filter #(fmt= % (:default-audio-format settings)) streams)
                streams)
              (first streams))
            :else (first video-streams))
      :content))

(defn get-audio-stream
  [{:keys [audio-streams video-streams]} settings]
  (-> (cond (seq audio-streams)
            (as-> audio-streams streams
              (remove #(= (:deliveryMethod %) "HLS") streams)
              (if (some #(fmt= % (:default-audio-format settings))
                        streams)
                (filter #(fmt= % (:default-audio-format settings)) streams)
                streams)
              (first streams))
            (seq video-streams)
            (as-> video-streams streams
              (if (some #(fmt= % (:default-video-format settings)) streams)
                (filter #(fmt= % (:default-video-format settings))
                        streams)
                streams)
              (if (some #(= (:resolution %)
                            (:default-resolution settings))
                        video-streams)
                (filter #(= (:resolution %)
                            (:default-resolution settings))
                        streams)
                streams)
              (first streams))
            :else (first audio-streams))
      :content))
