(ns tubo.player.components
  (:require
   ["media-chrome/dist/react" :refer
    (MediaController
     MediaControlBar
     MediaTimeRange
     MediaTimeDisplay
     MediaVolumeRange
     MediaFullscreenButton
     MediaPlayButton
     MediaMuteButton
     MediaLoadingIndicator
     MediaPreviewThumbnail
     MediaPreviewChapterDisplay
     MediaPreviewTimeDisplay)]
   ["media-chrome/dist/react/menu/index.js"
    :refer
    (MediaCaptionsMenu
     MediaPlaybackRateMenu
     MediaSettingsMenu
     MediaSettingsMenuButton
     MediaSettingsMenuItem
     MediaRenditionMenu)]
   ["motion/react" :refer [motion]]
   ["shaka-video-element/react$default" :as ShakaVideo]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.bookmarks.modals :as modals]
   [tubo.ui :as ui]
   [tubo.utils :as utils]))

(defn popover
  [{:keys [uploader-url uploader-name uploader-verified uploader-avatars]
    :as   stream} & {:keys [tooltip-classes]}]
  (let [queue     @(rf/subscribe [:queue])
        queue-pos @(rf/subscribe [:queue/position])
        bookmark  #(rf/dispatch [:modals/open
                                 [modals/add-to-bookmark %]])]
    [ui/popover
     [{:label             "Start radio"
       :icon              [:i.fa-solid.fa-tower-cell]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:bg-player/start-radio
                                         stream])}
      {:label             "Add current to playlist"
       :icon              [:i.fa-solid.fa-plus]
       :stop-propagation? true
       :on-click          #(bookmark stream)}
      {:label             "Add queue to playlist"
       :icon              [:i.fa-solid.fa-list]
       :stop-propagation? true
       :on-click          #(bookmark queue)}
      {:label             "Remove from queue"
       :icon              [:i.fa-solid.fa-trash]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:queue/remove queue-pos])}
      {:label             "Switch to main"
       :icon              [:i.fa-solid.fa-display]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:main-player/show])}
      (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
        {:label             "Unsubscribe from channel"
         :icon              [:i.fa-solid.fa-user-minus]
         :stop-propagation? true
         :on-click          #(rf/dispatch [:subscriptions/remove uploader-url])}
        {:label             "Subscribe to channel"
         :icon              [:i.fa-solid.fa-user-plus]
         :stop-propagation? true
         :on-click          #(rf/dispatch [:subscriptions/add
                                           {:url      uploader-url
                                            :name     uploader-name
                                            :verified uploader-verified
                                            :avatars  uploader-avatars}])})
      {:label             "Show channel details"
       :icon              [:i.fa-solid.fa-user]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:navigation/navigate
                                         {:name   :channel-page
                                          :params {}
                                          :query  {:url
                                                   uploader-url}}])}
      {:label             "Close player"
       :stop-propagation? true
       :icon              [:i.fa-solid.fa-close]
       :on-click          #(rf/dispatch [:bg-player/dispose])}]
     :stop-propagation? true
     :tooltip-classes
     (or tooltip-classes ["bottom-0"])]))

(defn button
  [& {:keys [icon on-click disabled? show-on-mobile? extra-classes]}]
  [:> motion.button
   {:whileTap {:scale [0.9 1]}
    :initial  {:scale 1}
    :class    (concat
               ui/common-button-classes
               (when disabled? ["opacity-50" "cursor-auto"])
               (if show-on-mobile?
                 ["flex"]
                 ["hidden" "lg:flex"])
               extra-classes)
    :disabled disabled?
    :on-click (fn [e]
                (.stopPropagation e)
                (on-click))}
   icon])

(defn loop-button
  [color show-on-mobile? & {:keys [extra-classes]}]
  (let [loop-playback @(rf/subscribe [:player/loop])]
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
     :extra-classes extra-classes
     :show-on-mobile? show-on-mobile?]))

(defn shuffle-button
  [color show-on-mobile? & {:keys [extra-classes]}]
  (let [shuffle? @(rf/subscribe [:player/shuffled])]
    [button
     :icon
     [:i.fa-solid.fa-shuffle {:style {:color (when shuffle? color)}}]
     :on-click #(rf/dispatch [:queue/shuffle (not shuffle?)])
     :extra-classes extra-classes
     :show-on-mobile? show-on-mobile?]))

(defn time-range
  [overlay-active?]
  [:> MediaTimeRange
   {:class
    ["w-full" "h-[5px]" (when-not overlay-active? "z-10")
     "[--media-range-thumb-height:13px]"
     "[--media-range-thumb-width:13px]"
     "[--media-range-track-height:3px]"
     "hover:[--media-range-track-height:5px]"
     "[--media-control-hover-background:transparent]"
     "[--media-range-track-background:rgba(255,255,255,.2)]"
     "[--media-range-track-pointer-background:rgba(255,255,255,.5)]"
     "[--media-time-range-buffered-color:rgba(255,255,255,.4)]"
     "[--media-range-bar-color:var(--service-color)]"
     "[--media-range-thumb-border-radius:13px]"
     "[--media-range-thumb-background:var(--service-color)]"]}
   [:> MediaPreviewThumbnail {:slot "preview"}]
   [:> MediaPreviewChapterDisplay {:slot "preview"}]
   [:> MediaPreviewTimeDisplay {:slot "preview" :class ["pt-0"]}]])

(def slider-classes
  ["h-[var(--height)]"
   "bg-[linear-gradient(to_right,var(--progress-color)_var(--value),var(--light-track-color)_var(--value))]"
   "dark:bg-[linear-gradient(to_right,var(--progress-color)_var(--value),var(--dark-track-color)_var(--value))]"
   "[&::-webkit-slider-runnable-track]:h-[var(--height)]"
   "[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,transparent_var(--value),var(--light-buffered-color)_var(--value),var(--light-buffered-color)_var(--buffered),var(--light-track-color)_var(--buffered))]"
   "dark:[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,transparent_var(--value),var(--dark-track-color)_var(--value),var(--dark-track-color)_var(--buffered),var(--dark-buffered-color)_var(--buffered))]"
   "[&::-webkit-slider-thumb]:appearance-none"
   "[&::-webkit-slider-thumb]:border-0"
   "[&::-webkit-slider-thumb]:rounded-full"
   "[&::-webkit-slider-thumb]:h-[var(--thumb-size)]"
   "[&::-webkit-slider-thumb]:w-[var(--thumb-size)]"
   "[&::-webkit-slider-thumb]:bg-[var(--thumb-color)]"
   "[&::-moz-range-track]:h-[var(--height)]"
   "[&::-moz-range-track]:bg-[linear-gradient(to_right,transparent_var(--value),var(--light-buffered-color)_var(--value),var(--light-buffered-color)_var(--buffered),var(--light-track-color)_var(--buffered))]"
   "dark:[&::-moz-range-track]:bg-[linear-gradient(to_right,transparent_var(--value),var(--dark-track-color)_var(--value),var(--dark-track-color)_var(--buffered),var(--dark-buffered-color)_var(--buffered))]"
   "[&::-moz-range-thumb]:appearance-none"
   "[&::-moz-range-thumb]:border-0"
   "[&::-moz-range-thumb]:rounded-full"
   "[&::-moz-range-thumb]:h-[var(--thumb-size)]"
   "[&::-moz-range-thumb]:w-[var(--thumb-size)]"
   "[&::-moz-range-thumb]:bg-[var(--thumb-color)]"])

(defn slider
  [&
   {:keys [progress-color light-track-color light-buffered-color
           dark-track-color dark-buffered-color height thumb-size thumb-color
           extra-classes rounded? on-input on-change max extra-styles value]
    :or   {progress-color       "white"
           light-track-color    "#d4d4d4"
           light-buffered-color "#a3a3a3"
           dark-track-color     "#737373"
           dark-buffered-color  "#525252"
           height               "0.5rem"
           thumb-size           "1rem"
           thumb-color          "white"}}]
  [:input.w-full.cursor-pointer.appearance-none.focus:outline-none
   {:style     (merge {"--height"               height
                       "--progress-color"       progress-color
                       "--thumb-size"           thumb-size
                       "--light-buffered-color" light-buffered-color
                       "--light-track-color"    light-track-color
                       "--dark-buffered-color"  dark-buffered-color
                       "--dark-track-color"     dark-track-color
                       "--thumb-color"          thumb-color
                       "--value"                (str (* (/ value max) 100) "%")}
                      extra-styles)
    :class     (concat slider-classes
                       extra-classes
                       (when rounded?
                         ["rounded-full" "[&::-moz-range-track]:rounded-full"
                          "[&::-webkit-slider-runnable-track]:rounded-full"]))
    :type      "range"
    :on-click  #(.stopPropagation %)
    :on-input  on-input
    :on-change (or on-change (constantly nil))
    :max       max
    :value     value}])

(defn time-slider
  [!player !elapsed-time & extra-args]
  (let [bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !buffered        @(rf/subscribe [:bg-player/buffered])
        max-value        (if (and bg-player-ready?
                                  @!player
                                  (not (js/isNaN (.-duration @!player))))
                           (.floor js/Math (.-duration @!player))
                           100)]
    (apply slider
           :extra-styles
           {"--buffered" (str @!buffered "%")}
           :on-input
           #(reset! !elapsed-time (.. % -target -value))
           :on-change
           #(when (and bg-player-ready? @!player)
              (set! (.-currentTime @!player) @!elapsed-time))
           :max
           max-value
           :value
           @!elapsed-time
           extra-args)))

(defn volume-slider
  [player & extra-args]
  (let [volume @(rf/subscribe [:player/volume])]
    (apply
     slider
     :max
     100
     :value
     volume
     :on-input
     #(rf/dispatch [:player/change-volume (.. % -target -value) player])
     extra-args)))

(defn track-button
  [& {:keys [classes type]}]
  (let [show-main-player? @(rf/subscribe [:main-player/show])
        queue             @(rf/subscribe [:queue])
        position          @(rf/subscribe [:queue/position])
        target-pos        ((if (= type :previous) dec inc) position)
        disabled?         (not (and show-main-player? (get queue target-pos)))]
    [:button.cursor-pointer
     {:class    (concat classes (when disabled? ["opacity-50" "cursor-auto"]))
      :on-click #(rf/dispatch [:queue/change-pos target-pos])
      :disabled disabled?}
     [:svg.w-full.h-full
      {:class   ["fill-[var(--media-primary-color)]"]
       :slot    "icon"
       :viewBox "0 0 36 36"}
      [:use.stroke-2.stroke.fill-none
       {:class     "stroke-black/15"
        :xlinkHref (str "#" (if (= type :previous) "prev-icon" "next-icon"))}]
      [:path#prev-icon
       {:id (if (= type :previous) "prev-icon" "next-icon")
        :d  (if (= type :previous)
              "m 12,12 h 2 v 12 h -2 z m 3.5,6 8.5,6 V 12 z"
              "M 12,24 20.5,18 12,12 V 24 z M 22,12 v 12 h 2 V 12 h -2 z")}]]]))

(defn seek-mobile-button
  []
  (let [!show? (r/atom nil)]
    (fn [!player & {:keys [type]}]
      [:div.select-none.h-full.absolute.flex.items-center.justify-center
       {:class    (concat ["w-1/4"]
                          (if (= type :backward)
                            ["rounded-tr-[100%400px]" "rounded-br-[100%400px]"
                             "left-0"]
                            ["rounded-tl-[100%400px]" "rounded-bl-[100%400px]"
                             "right-0"])
                          (when @!show?
                            ["bg-black/50"]))
        :on-click (fn [e]
                    (when (= (.-detail e) 2)
                      (reset! !show? true)
                      (js/setTimeout #(reset! !show? false) 500)
                      (rf/dispatch [:player/seek
                                    ((if (= type :backward) - +)
                                     (.-currentTime @!player)
                                     10)
                                    !player])))}
       (when @!show?
         [:> motion.button
          {:class      ["flex" "flex-col" "gap-y-4" "text-white"]
           :animate    {:x (if (= type :backward) -5 5)}
           :initial    {:x 0}
           :transition {:ease "easeIn" :duration 0.2}}
          (if (= type :backward)
            [:i.fa-solid.fa-backward]
            [:i.fa-solid.fa-forward])
          [:span.font-bold 10]])])))

(defn mobile-controls
  [!player overlay-active?]
  (let [classes
        ["bg-black/50" "rounded-full" "select-none" "aspect-[1]"
         "[--media-button-icon-height:36px]"
         "[--media-button-icon-width:36px]"
         "[--media-button-icon-color:var(--media-primary-color,#fff)]"]]
    [:<>
     [:div.flex.self-stretch.items-center.justify-center.h-full.w-full.flex-row.flex-wrap.gap-4.z-10
      {:slot "centered-chrome"
       :class
       ["group-[&[breakpointmd]:not([mediaisfullscreen])]:hidden"
        "group-[&[mediaisfullscreen][breakpointxl]]:hidden" "bg-black/40"]}
      (when overlay-active?
        [:div.absolute.h-full.w-full.flex.z-10.left-0])
      [seek-mobile-button !player :type :backward]
      [track-button :type :previous :classes (into classes ["w-12"])]
      [:> MediaPlayButton
       {:class     (into classes ["w-16"])
        :notooltip true}]
      [track-button :type :next :classes (into classes ["w-12"])]
      [seek-mobile-button !player :type :forward]]]))

(defn control-bar
  [overlay-active?]
  (let [show-main-player? @(rf/subscribe [:main-player/show])]
    [:> MediaControlBar
     {:class ["relative" "pl-[10px]" "pr-[5px]"
              (when-not overlay-active? "z-10")
              "[--media-range-thumb-height:13px]"
              "[--media-range-thumb-width:13px]"
              "[--media-range-thumb-border-radius:13px]"
              "[--media-range-track-height:3px]"
              "[--media-control-hover-background:transparent]"
              "[--media-tooltip-display:none]"]}
     (when show-main-player?
       [track-button :type :previous :classes ["h-11" "hidden" "xs:flex"]])
     [:> MediaPlayButton
      {:mediapaused true
       :class       ["group-[&[breakpointmd]]:block"]}]
     (when show-main-player?
       [track-button :type :next :classes ["h-11" "hidden" "xs:flex"]])
     [:> MediaMuteButton
      {:class "peer/mute"}]
     [:> MediaVolumeRange
      {:class
       ["w-0" "overflow-hidden" "transition-[width]"
        "transition-200" "ease-in" "peer-hover/mute:w-[70px]"
        "peer-focus/mute:w-[70px]" "hover:w-[70px]" "focus:w-[70px]"
        "[--media-range-track-background:rgba(255,255,255,.2)]"
        "[--media-range-bar-color:var(--service-color)]"
        "[--media-range-thumb-background:var(--service-color)]"]}]
     [:> MediaTimeDisplay {:showDuration true}]
     [:span.control-spacer.grow]
     [:> MediaSettingsMenuButton
      {:class ["group/settings" "relative" "inline-block" "w-9"
               "group-[&[breakpointmd]]:w-12"
               "group-[&[mediaisfullscreen]]:w-[54px]" "h-full" "px-0.5"
               "py-0" "opacity-90"]}
      [:svg.w-full.h-full.rotate-0.ease-in.transition-transform.duration-100
       {:class   ["fill-[var(--media-primary-color)]"
                  "group-aria-expanded/settings:rotate-[30deg]"]
        :slot    "icon"
        :viewBox "0 0 36 36"}
       [:use.stroke-2.stroke.fill-none
        {:class "stroke-black/15" :xlinkHref "#settings-icon"}]
       [:path#settings-icon
        {:d
         "M11.8153 12.0477L14.2235 12.9602C14.6231 12.6567 15.0599 12.3996 15.5258 12.1971L15.9379 9.66561C16.5985 9.50273 17.2891 9.41632 18 9.41632C18.7109 9.41632 19.4016 9.50275 20.0622 9.66566L20.4676 12.1555C20.9584 12.3591 21.418 12.6227 21.8372 12.9372L24.1846 12.0477C25.1391 13.0392 25.8574 14.2597 26.249 15.6186L24.3196 17.1948C24.3531 17.4585 24.3704 17.7272 24.3704 18C24.3704 18.2727 24.3531 18.5415 24.3196 18.8051L26.249 20.3814C25.8574 21.7403 25.1391 22.9607 24.1846 23.9522L21.8372 23.0628C21.4179 23.3772 20.9584 23.6408 20.4676 23.8445L20.0622 26.3343C19.4016 26.4972 18.7109 26.5836 18 26.5836C17.2891 26.5836 16.5985 26.4972 15.9379 26.3344L15.5258 23.8029C15.0599 23.6003 14.6231 23.3433 14.2236 23.0398L11.8154 23.9523C10.8609 22.9608 10.1426 21.7404 9.75098 20.3815L11.7633 18.7375C11.7352 18.4955 11.7208 18.2495 11.7208 18C11.7208 17.7505 11.7352 17.5044 11.7633 17.2625L9.75098 15.6185C10.1426 14.2596 10.8609 13.0392 11.8153 12.0477ZM18 20.75C19.5188 20.75 20.75 19.5188 20.75 18C20.75 16.4812 19.5188 15.25 18 15.25C16.4812 15.25 15.25 16.4812 15.25 18C15.25 19.5188 16.4812 20.75 18 20.75Z"}]]]
     [:> MediaFullscreenButton]]))

(defn settings-menu
  []
  [:> MediaSettingsMenu
   {:anchor "auto"
    :hidden true
    :class
    ["absolute" "rounded-xl" "right-3" "bottom-[61px]" "z-50"
     "select-none" "will-change-[width,height]"
     "[text-shadow:_0_0_2px_rgba(0,0,0,0.5)]"
     "[--media-settings-menu-min-width:220px]"
     "[--media-secondary-color:rgba(28,28,28,.9)]"
     "group-[&[mediaisfullscreen][breakpointxl]]:[--media-settings-menu-min-width:320px]"]}
   (let
     [classes
      ["text-[13px]" "font-medium" "h-10" "[&[submenusize='0']]:hidden"
       "group-[&[mediaisfullscreen][breakpointxl]]:text-[16px]"
       "group-[&[mediaisfullscreen][breakpointxl]]:h-[50px]"]
      args {:slot "submenu" :hidden true}]
     [:<>
      [:> MediaSettingsMenuItem
       {:class classes}
       "Playback Speed"
       [:> MediaPlaybackRateMenu args
        [:div {:slot "title"} "Speed"]]]
      [:> MediaSettingsMenuItem
       {:class classes}
       "Quality"
       [:> MediaRenditionMenu args
        [:div {:slot "title"} "Quality"]]]
      [:> MediaSettingsMenuItem
       {:class classes}
       "Subtitles/CC"
       [:> MediaCaptionsMenu args
        [:div {:slot "title"} "Subtitles/CC"]]]])])


(defn video-player
  [_ _ _ on-mount on-unmount]
  (let [!user-inactive (r/atom nil)
        !media-paused  (r/atom nil)
        !controller    (atom nil)
        on-inactive    #(reset! !user-inactive (.-detail %))
        on-paused      #(reset! !media-paused (.-detail %))]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (when-let [el @!controller]
          (.addEventListener el "userinactivechange" on-inactive)
          (.addEventListener el "mediapaused" on-paused))
        (on-mount))
      :component-will-unmount on-unmount
      :reagent-render
      (fn [{:keys [thumbnail subtitles service-id] :as stream} !player
           video-args]
        (let [service-color   (utils/get-service-color service-id)
              settings        @(rf/subscribe [:settings])
              overlay-active? (and (or (nil? @!user-inactive) @!user-inactive)
                                   (not @!media-paused))]
          [:> MediaController
           {:ref   #(reset! !controller %)
            :style {"--service-color" service-color}
            :class ["group" "font-roboto" "text-[13px]" "min-h-full" "h-full"
                    "md:rounded-[0.9rem]" "overflow-hidden"
                    "w-full" "aspect-video"
                    "[&[mediaisfullscreen]]:text-[17px]"
                    "[--media-background-color:black]"
                    "[--media-primary-color:white]"
                    "[--media-secondary-color:transparent]"
                    "[--media-font-family:Roboto,sans-serif]"]}
           [:> ShakaVideo
            (into
             {:poster         thumbnail
              :playsInline    true
              :ref            #(reset! !player %)
              :slot           "media"
              :on-loaded-data #(rf/dispatch [:player/start !player stream])
              :preload        "metadata"}
             video-args)
            [:track
             {:label   (:display-language-name (first subtitles))
              :kind    "captions"
              :srcLang (:language-tag (first subtitles))
              :src     (str (:instance settings)
                            "/proxy/"
                            (js/encodeURIComponent (:content (first
                                                              subtitles))))}]]
           [:div.absolute.w-full.bottom-0.pointer-events-none.bg-bottom.bg-repeat-x
            {:class ["md:rounded-b-xl" "pt-[37px]" "h-[170px]"]
             :style
             {"backgroundImage"
              "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAACqCAYAAABsziWkAAAAAXNSR0IArs4c6QAAAQVJREFUOE9lyNdHBQAAhfHb3nvvuu2997jNe29TJJEkkkgSSSSJJJJEEkkiifRH5jsP56Xz8PM5gcC/xfDEmjhKxEOCSaREEiSbFEqkQppJpzJMJiWyINvkUCIX8kw+JQqg0BRRxaaEEqVQZsopUQGVpooS1VBjglStqaNEPTSYRko0QbNpoUQrtJl2qsN0UqILuk0PJXqhz/RTYgAGzRA1bEYoMQpjZpwSExAyk5SYgmkzQ82aOUqEIWKilJiHBbNIiSVYhhVYhTVYhw3YhC3Yhh3YhT3YhwM4hCM4hhM4hTM4hwu4hCu4hhu4hTu4hwd4hCd4hhd4hTd4hw/4hC/4hh/4/QM2/id28uIEJAAAAABJRU5ErkJggg==')"}}]
           [:> MediaLoadingIndicator
            {:class      ["absolute"
                          "[--media-loading-indicator-icon-height:200px]"]
             :slot       "centered-chrome"
             :noautohide true}]
           [mobile-controls !player overlay-active?]
           [time-range overlay-active?]
           [control-bar overlay-active?]
           [settings-menu]]))})))

(defn on-progress
  [!player !buffered]
  (let [len (.. @!player -buffered -length)]
    (when (and (.-duration @!player) (> len 0))
      (if (= (.end (.-buffered @!player) (- len 1)) (.-duration @!player))
        (reset! !buffered 100)
        (when (< (.start (.-buffered @!player) (- len 1))
                 (.-currentTime @!player))
          (reset! !buffered (* (/ (.end (.-buffered @!player) (- len 1))
                                  (.-duration @!player))
                               100)))))))

(defn on-update
  [!player !buffered !elapsed]
  (when @(rf/subscribe [:bg-player/waiting])
    (rf/dispatch [:bg-player/set-waiting false]))
  (on-progress !player !buffered)
  (reset! !elapsed (.-currentTime @!player)))

(defn audio-player
  [!player]
  (let [!elapsed  @(rf/subscribe [:elapsed-time])
        !paused   @(rf/subscribe [:player/paused])
        pos       @(rf/subscribe [:queue/position])
        stream    @(rf/subscribe [:queue/current])
        !buffered @(rf/subscribe [:bg-player/buffered])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:bg-player/unmount])
      :component-did-mount #(rf/dispatch [:bg-player/mount stream !player pos])
      :reagent-render
      (fn [!player]
        (let [stream @(rf/subscribe [:queue/current])]
          [:> ShakaVideo
           {:style          {:display "none"}
            :ref            #(reset! !player %)
            :preload        "metadata"
            :playsInline    true
            :on-play        #(do (reset! !paused false)
                                 (rf/dispatch [:player/set-playback-state
                                               "playing"]))
            :on-pause       #(reset! !paused true)
            :on-waiting     #(rf/dispatch [:bg-player/set-waiting true])
            :loop           (= @(rf/subscribe [:player/loop]) :stream)
            :muted          @(rf/subscribe [:player/muted])
            :on-can-play    #(rf/dispatch [:bg-player/set-ready true])
            :on-seeked      #(reset! !elapsed (.-currentTime @!player))
            :on-progress    #(on-progress !player !buffered)
            :on-time-update #(on-update !player !buffered !elapsed)
            :on-loaded-data #(rf/dispatch [:player/start !player stream])
            :on-error       #(rf/dispatch [:player/on-error !player])}]))})))
