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
     MediaMuteButton
     MediaLoadingIndicator
     MediaSeekBackwardButton
     MediaPlayButton
     MediaSeekForwardButton)]
   ["media-chrome/dist/react/menu/index.js"
    :refer
    (MediaCaptionsMenu
     MediaPlaybackRateMenu
     MediaSettingsMenu
     MediaSettingsMenuButton
     MediaSettingsMenuItem
     MediaRenditionMenu)]
   [tubo.utils :as utils]))

(defn button
  [& {:keys [icon on-click disabled? show-on-mobile? extra-classes]}]
  [:button.outline-none.focus:ring-transparent
   {:class    (into (into (when disabled? [:opacity-50 :cursor-auto])
                          (when-not show-on-mobile?
                            [:hidden :lg:flex :items-center :justify-center]))
                    extra-classes)
    :disabled disabled?
    :on-click (fn [e]
                (.stopPropagation e)
                (on-click))}
   icon])

(defn loop-button
  [loop-playback color show-on-mobile?]
  [button
   :icon
   [:div.relative.flex.items-center
    [:i.fa-solid.fa-repeat
     {:style {:color (when loop-playback color)}}]
    (when (= loop-playback :stream)
      [:div.absolute.w-full.h-full.flex.justify-center.items-center.font-bold
       {:class "text-[6px]"
        :style {:color (when loop-playback color)}}
       "1"])]
   :on-click #(rf/dispatch [:player/loop])
   :extra-classes [:text-sm]
   :show-on-mobile? show-on-mobile?])

(defn shuffle-button
  [shuffle? color show-on-mobile?]
  [button
   :icon
   [:i.fa-solid.fa-shuffle {:style {:color (when shuffle? color)}}]
   :on-click #(rf/dispatch [:queue/shuffle (not shuffle?)])
   :extra-classes [:text-sm]
   :show-on-mobile? show-on-mobile?])

(defn video-player
  [_ _ _ on-mount]
  (r/create-class
   {:component-did-mount on-mount
    :reagent-render
    (fn [{:keys [thumbnail subtitles service-id]} !player video-args]
      (let [service-color (utils/get-service-color service-id)]
        [:> MediaController
         {:style {"--media-secondary-color" "transparent"
                  "--media-primary-color"   "white"
                  "aspectRatio"             "16/9"
                  "height"                  "100%"
                  "width"                   "100%"}
          :class "md:rounded-xl"}
         [:video
          (into
           {:style   {"maxHeight" "100%"
                      "minHeight" "100%"
                      "minWidth"  "100%"
                      "maxWidth"  "100%"}
            :poster  thumbnail
            :ref     #(reset! !player %)
            :slot    "media"
            :preload "metadata"
            :class   "md:rounded-xl"}
           video-args)
          [:track
           {:label   (:displayLanguageName (first subtitles))
            :kind    "captions"
            :srcLang (:languageTag (first subtitles))
            :src     (:content (first subtitles))}]]
         [:div.ytp-gradient-bottom.absolute.w-full.bottom-0.pointer-events-none.bg-bottom.bg-repeat-x
          {:style
           {"paddingTop" "37px"
            "height" "170px"
            "backgroundImage"
            "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAACqCAYAAABsziWkAAAAAXNSR0IArs4c6QAAAQVJREFUOE9lyNdHBQAAhfHb3nvvuu2997jNe29TJJEkkkgSSSSJJJJEEkkiifRH5jsP56Xz8PM5gcC/xfDEmjhKxEOCSaREEiSbFEqkQppJpzJMJiWyINvkUCIX8kw+JQqg0BRRxaaEEqVQZsopUQGVpooS1VBjglStqaNEPTSYRko0QbNpoUQrtJl2qsN0UqILuk0PJXqhz/RTYgAGzRA1bEYoMQpjZpwSExAyk5SYgmkzQ82aOUqEIWKilJiHBbNIiSVYhhVYhTVYhw3YhC3Yhh3YhT3YhwM4hCM4hhM4hTM4hwu4hCu4hhu4hTu4hwd4hCd4hhd4hTd4hw/4hC/4hh/4/QM2/id28uIEJAAAAABJRU5ErkJggg==')"}
           :class "md:rounded-b-xl"}]
         [:> MediaTimeRange
          {:class ["w-full" "h-[5px]"]
           :style
           {"--media-control-hover-background"       "transparent"
            "--media-range-track-transition"         "height 0.1s linear"
            "--media-range-track-background"         "rgba(255,255,255,.2)"
            "--media-range-track-pointer-background" "rgba(255,255,255,.5)"
            "--media-time-range-buffered-color"      "rgba(255,255,255,.4)"
            "--media-range-bar-color"                service-color
            "--media-range-thumb-border-radius"      "13px"
            "--media-range-thumb-background"         service-color
            "--media-range-thumb-transition"         "transform 0.1s linear"}}]
         [:> MediaLoadingIndicator
          {:slot       "centered-chrome"
           :noautohide true
           :style      {"--media-loading-indicator-icon-height" "200px"
                        "position"                              "absolute"}}]
         [:div.block.sm:hidden {:slot "centered-chrome"}
          [:> MediaSeekBackwardButton
           {:seekoffset "15"
            :style      {"--media-button-icon-height"       "30px"
                         "--media-control-hover-background" "transparent"}
            :notooltip  true}]
          [:> MediaPlayButton
           {:style     {"--media-button-icon-height"       "50px"
                        "--media-control-hover-background" "transparent"}
            :notooltip true}]
          [:> MediaSeekForwardButton
           {:seekoffset "15"
            :style      {"--media-button-icon-height"       "30px"
                         "--media-control-hover-background" "transparent"}
            :notooltip  true}]]
         [:> MediaSettingsMenu
          {:anchor "auto"
           :hidden true
           :style  {"--media-secondary-color"          "rgba(23, 23, 23, .9)"
                    "--media-menu-border"              "1px solid white"
                    "--media-menu-border-radius"       "35px"
                    "--media-font-family"              "Nunito Sans"
                    "--media-control-hover-background" "red"}}
          [:> MediaSettingsMenuItem
           "Speed"
           [:> MediaPlaybackRateMenu
            {:slot "submenu" :hidden true}
            [:div {:slot "title"} "Speed"]]]
          [:> MediaSettingsMenuItem
           "Quality"
           [:> MediaRenditionMenu
            {:slot "submenu" :hidden true}
            [:div {:slot "title"} "Quality"]]]
          [:> MediaSettingsMenuItem
           "Captions"
           [:> MediaCaptionsMenu
            {:slot "submenu" :hidden true}
            [:div {:slot "title"} "Captions"]]]]
         [:> MediaControlBar
          {:class "relative pl-[10px] pr-[5px]"
           :style
           {"--media-control-hover-background"  "transparent"
            "--media-range-track-height"        "3px"
            "--media-range-thumb-height"        "13px"
            "--media-range-thumb-width"         "13px"
            "--media-range-thumb-border-radius" "13px"
            "--media-tooltip-display"           "none"}}
          [:> MediaPlayButton
           {:class "hidden sm:block py-[6px] px-[10px]"
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
          [:> MediaSettingsMenuButton]
          [:> MediaPipButton]
          [:> MediaFullscreenButton]]]))}))
