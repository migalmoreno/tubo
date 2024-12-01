(ns tubo.player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["@vidstack/react" :refer (MediaPlayer MediaProvider Poster)]
   ["@vidstack/react/player/layouts/default" :refer
    (defaultLayoutIcons DefaultVideoLayout)]))

(defn get-video-player-sources
  [available-streams service-id]
  (if available-streams
    (if (= service-id 3)
      (map (fn [{:keys [content]}] {:src content :type "video/mp4"})
           (reverse available-streams))
      (->> available-streams
           (filter #(and (not= (:format %) "WEBMA_OPUS")
                         (not= (:format %) "OPUS")
                         (not= (:format %) "M4A")))
           (sort-by :bitrate)
           (#(if (empty? (filter (fn [x] (= (:format x) "MP3")) %))
               (reverse %)
               %))
           (map (fn [{:keys [content]}] {:src content :type "video/mp4"}))
           first))
    []))

(defn video-player
  [_stream _!player]
  (let [!elapsed-time       @(rf/subscribe [:elapsed-time])
        !main-player-first? (r/atom true)]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:main-player/ready false])
      :reagent-render
      (fn [{:keys [name video-streams audio-streams thumbnail-url service-id]}
           !player]
        (let [show-main-player? @(rf/subscribe [:main-player/show])]
          [:> MediaPlayer
           {:title          name
            :src            (get-video-player-sources (into video-streams
                                                            audio-streams)
                                                      service-id)
            :poster         thumbnail-url
            :class          "w-full xl:w-3/5 overflow-hidden"
            :playsInline    true
            :ref            #(reset! !player %)
            :loop           (when show-main-player?
                              (= @(rf/subscribe [:player/loop]) :stream))
            :onSeeked       (when show-main-player?
                              #(reset! !elapsed-time (.-currentTime @!player)))
            :onTimeUpdate   (when show-main-player?
                              #(reset! !elapsed-time (.-currentTime @!player)))
            :onEnded        #(when show-main-player?
                               (rf/dispatch [:queue/change-pos
                                             (inc @(rf/subscribe
                                                    [:queue/position]))])
                               (reset! !elapsed-time 0))
            :onLoadedData   (fn []
                              (when show-main-player?
                                (rf/dispatch [:main-player/start]))
                              (when (and @!main-player-first? show-main-player?)
                                (reset! !main-player-first? false)))
            :onPlay         #(rf/dispatch [:main-player/play])
            :onCanPlay      #(rf/dispatch [:main-player/ready true])
            :onSourceChange #(when-not @!main-player-first?
                               (reset! !elapsed-time 0))}
           [:> MediaProvider
            [:> Poster
             {:src   thumbnail-url
              :alt   name
              :class :vds-poster}]]
           [:> DefaultVideoLayout {:icons defaultLayoutIcons}]]))})))
