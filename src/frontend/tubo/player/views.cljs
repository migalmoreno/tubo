(ns tubo.player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["media-chrome/dist/react" :refer
    (MediaController
     MediaControlBar
     MediaTimeRange
     MediaTimeDisplay
     MediaVolumeRange
     MediaFullscreenButton
     MediaPipButton
     MediaPlayButton
     MediaPlaybackRateButton
     MediaMuteButton)]))

(defn video-player
  [_stream _!player]
  (let [!elapsed-time       @(rf/subscribe [:elapsed-time])
        !main-player-first? (r/atom true)]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:main-player/ready false])
      :reagent-render
      (fn [{:keys [video-streams audio-streams thumbnails]}
           !player]
        (let [show-main-player? @(rf/subscribe [:main-player/show])
              service-color     @(rf/subscribe [:service-color])]
          [:div
           {:class "w-full h-80 md:h-[450px] lg:h-[600px]"}
           [:> MediaController
            {:style {"--media-secondary-color" "transparent"
                     "--media-primary-color"   "white"
                     "aspectRatio"             "16/9"
                     "height"                  "100%"
                     "width"                   "100%"}}
            [:video
             {:style          {"maxHeight" "100%"
                               "minHeight" "100%"
                               "minWidth"  "100%"
                               "maxWidth"  "100%"}
              :ref            #(reset! !player %)
              :poster         (-> thumbnails
                                  last
                                  :url)
              :loop           (when show-main-player?
                                (= @(rf/subscribe [:player/loop]) :stream))
              :on-can-play    #(rf/dispatch [:main-player/ready true])
              :on-ended       #(when show-main-player?
                                 (rf/dispatch [:queue/change-pos
                                               (inc @(rf/subscribe
                                                      [:queue/position]))])
                                 (reset! !elapsed-time 0))
              :on-play        #(rf/dispatch [:main-player/play])
              :on-loaded-data (fn []
                                (when show-main-player?
                                  (rf/dispatch [:main-player/start]))
                                (when (and @!main-player-first?
                                           show-main-player?)
                                  (reset! !main-player-first? false)))
              :on-time-update (when show-main-player?
                                #(reset! !elapsed-time (.-currentTime
                                                        @!player)))
              :on-seeked      (when show-main-player?
                                #(reset! !elapsed-time (.-currentTime
                                                        @!player)))
              :slot           "media"

              :src            (-> (into video-streams audio-streams)
                                  first
                                  :content)
              :preload        "auto"
              :muted          @(rf/subscribe [:player/muted])}]
            [:div.ytp-gradient-bottom.absolute.w-full.bottom-0.pointer-events-none.bg-bottom.bg-repeat-x
             {:style
              {"paddingTop" "37px"
               "height" "170px"
               "backgroundImage"
               "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAACqCAYAAABsziWkAAAAAXNSR0IArs4c6QAAAQVJREFUOE9lyNdHBQAAhfHb3nvvuu2997jNe29TJJEkkkgSSSSJJJJEEkkiifRH5jsP56Xz8PM5gcC/xfDEmjhKxEOCSaREEiSbFEqkQppJpzJMJiWyINvkUCIX8kw+JQqg0BRRxaaEEqVQZsopUQGVpooS1VBjglStqaNEPTSYRko0QbNpoUQrtJl2qsN0UqILuk0PJXqhz/RTYgAGzRA1bEYoMQpjZpwSExAyk5SYgmkzQ82aOUqEIWKilJiHBbNIiSVYhhVYhTVYhw3YhC3Yhh3YhT3YhwM4hCM4hhM4hTM4hwu4hCu4hhu4hTu4hwd4hCd4hhd4hTd4hw/4hC/4hh/4/QM2/id28uIEJAAAAABJRU5ErkJggg==')"}}]
            [:> MediaTimeRange
             {:class "w-full h-[5px]"
              :style
              {"--media-control-hover-background" "transparent"
               "--media-range-track-transition" "height 0.1s linear"
               "--media-range-track-background" "rgba(255,255,255,.2)"
               "--media-range-track-pointer-background" "rgba(255,255,255,.5)"
               "--media-time-range-buffered-color" "rgba(255,255,255,.4)"
               "--media-range-bar-color" service-color
               "--media-range-thumb-border-radius" "13px"
               "--media-range-thumb-background" service-color
               "--media-range-thumb-transition" "transform 0.1s linear"
               "--media-range-thumb-transform" "scale(0) translate(0%, 0%)"}}]
            [:> MediaControlBar
             {:class "relative pl-[10px] pr-[5px]"
              :style
              {"--media-control-hover-background"  "transparent"
               "--media-range-track-height"        "3px"
               "--media-range-thumb-height"        "13px"
               "--media-range-thumb-width"         "13px"
               "--media-range-thumb-border-radius" "13px"}}
             [:> MediaPlayButton
              {:class "py-[6px] px-[10px]"
               :style
               {"--media-button-icon-width" "30px"}}]
             [:> MediaMuteButton
              {:class "peer/mute"}]
             [:> MediaVolumeRange
              {:class
               ["w-0" "overflow-hidden" "transition-[width]"
                "transition-200" "ease-in" "peer-hover/mute:w-[70px]"
                "peer-focus/mute:w-[70px]" "hover:w-[70px]" "focus:w-[70px]"]
               :style
               {"--media-range-track-background" "rgba(255,255,255,.2)"
                "--media-range-bar-color"        service-color
                "--media-range-thumb-background" service-color}}]
             [:> MediaTimeDisplay {:showDuration true}]
             [:span.control-spacer.grow]
             [:> MediaPlaybackRateButton]
             [:> MediaPipButton]
             [:> MediaFullscreenButton]]]]))})))
