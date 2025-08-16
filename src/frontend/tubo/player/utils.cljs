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
            (as-> video-streams $
              (if (some #(fmt= % (:default-video-format settings)) $)
                (filter #(fmt= % (:default-video-format settings))
                        $)
                $)
              (if (some #(= (:resolution %)
                            (:default-resolution settings))
                        video-streams)
                (filter #(= (:resolution %)
                            (:default-resolution settings))
                        $)
                $)
              (first $))
            (seq audio-streams)
            (as-> audio-streams $
              (remove #(= (:deliveryMethod %) "HLS") $)
              (if (some #(fmt= % (:default-audio-format settings))
                        $)
                (filter #(fmt= % (:default-audio-format settings)) $)
                $)
              (first $))
            :else (first video-streams))
      :content))

(defn get-audio-stream
  [{:keys [audio-streams video-streams]} settings]
  (-> (cond (seq audio-streams)
            (as-> audio-streams $
              (remove #(= (:deliveryMethod %) "HLS") $)
              (if (some #(fmt= % (:default-audio-format settings))
                        $)
                (filter #(fmt= % (:default-audio-format settings)) $)
                $)
              (first $))
            (seq video-streams)
            (as-> video-streams $
              (if (some #(fmt= % (:default-video-format settings)) $)
                (filter #(fmt= % (:default-video-format settings))
                        $)
                $)
              (if (some #(= (:resolution %)
                            (:default-resolution settings))
                        video-streams)
                (filter #(= (:resolution %)
                            (:default-resolution settings))
                        $)
                $)
              (first $))
            :else (first audio-streams))
      :content))
