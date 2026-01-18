(ns tubo.bg-player.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.layout.views :as layout]
   [tubo.main-player.views :as main-player]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

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
   "dark:[&::-moz-range-track]:bg-[linear-gradient(to_right,transparent_var(--value),var(--dark-buffered-color)_var(--value),var(--dark-buffered-color)_var(--buffered),var(--dark-track-color)_var(--buffered))]"
   "[&::-moz-range-thumb]:appearance-none"
   "[&::-moz-range-thumb]:border-0"
   "[&::-moz-range-thumb]:rounded-full"
   "[&::-moz-range-thumb]:h-[--thumb-size]"
   "[&::-moz-range-thumb]:w-[--thumb-size]"
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

(defn metadata
  [{:keys [name uploader-name] :as stream}]
  [:div.flex.lg:flex-1.gap-x-4
   [:div
    [layout/thumbnail (dissoc stream :duration) nil :container-classes
     ["h-12" "w-12"] :image-classes ["rounded"]]]
   [:div.flex.flex-col.pr-4.gap-y-1
    [:h1.text-sm.line-clamp-1.w-fit
     {:title name}
     name]
    [:h1.text-xs.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit
     {:title uploader-name}
     uploader-name]]])

(defn main-controls
  [!player color]
  (let [queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])
        loading?         @(rf/subscribe [:bg-player/loading])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        paused?          @(rf/subscribe [:player/paused])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !elapsed-time    @(rf/subscribe [:elapsed-time])
        dark-theme       @(rf/subscribe [:dark-theme])]
    [:div.flex.flex-col.items-center.ml-auto.gap-y-2
     [:div.flex.justify-end.gap-x-4.items-center
      [player/loop-button color false :extra-classes ["text-sm"]]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/previous])
       :disabled? (not (and queue (not= queue-pos 0)))]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])]
      [player/button
       :icon
       (if (and (not loading?)
                (not waiting?)
                (or (nil? bg-player-ready?) @!player))
         (if paused?
           [:i.fa-solid.fa-play-circle]
           [:i.fa-solid.fa-pause-circle])
         [layout/loading-icon color "text-3xl lg:text-4xl"])
       :on-click #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes
       ["text-3xl" "lg:text-4xl" "w-[3rem]" "lg:w-[2.5rem]" "!p-0"
        "!bg-transparent"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/next])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]
      [player/shuffle-button color false :extra-classes ["text-sm"]]]
     [:div.hidden.lg:flex.items-center.gap-x-2
      {:class "text-[0.8rem]"}
      [:span.w-16.flex.justify-end
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [:div.w-20.lg:w-96.mx-2.flex.items-center
       {:style {"--thumb-bg" (if dark-theme
                               "rgb(212,212,212)"
                               "rgb(0,0,0)")}}
       [time-slider !player !elapsed-time :progress-color color :rounded? true
        :thumb-size "0.5rem" :thumb-color color]]
      [:span.w-16.flex.justify-start
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]]))

(defn extra-controls
  [!player color]
  (let [muted?     @(rf/subscribe [:player/muted])
        dark-theme @(rf/subscribe [:dark-theme])]
    [:div.flex.lg:justify-end.lg:flex-1.gap-x-2
     [:div.hidden.lg:flex.w-36.items-center.gap-x-4
      {:style {"--thumb-bg"
               (if dark-theme "rgb(212,212,212)" "rgb(0,0,0)")}}
      [player/button
       :icon
       (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
       :on-click #(rf/dispatch [:bg-player/mute (not muted?) !player])
       :show-on-mobile? true]
      [volume-slider !player :progress-color color :height "0.375rem"
       :thumb-size
       "0.375rem" :thumb-color color :rounded? true]]
     [layout/panel-popover
      [:div.flex {:class ["h-[calc(100dvh-92px)]"]}
       [main-player/player]]
      :stop-propagation? true
      :mobile-only? true
      :extra-classes ["w-10"]
      :icon
      [:i.fa-solid.fa-display]]
     [player/button
      :icon [:i.fa-solid.fa-up-right-and-down-left-from-center]
      :on-click #(rf/dispatch [:queue/show true])
      :show-on-mobile? true
      :extra-classes
      ["text-lg" "lg:text-base" "hidden" "lg:block"]]]))

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
        pos       @(rf/subscribe [:queue/position])
        stream    @(rf/subscribe [:queue/current])
        !buffered @(rf/subscribe [:bg-player/buffered])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:bg-player/unmount])
      :component-did-mount #(rf/dispatch [:bg-player/mount stream !player pos])
      :reagent-render
      (fn [!player]
        (let [stream @(rf/subscribe [:queue/current])]
          [:audio
           {:ref            #(reset! !player %)
            :preload        "metadata"
            :on-waiting     #(rf/dispatch [:bg-player/set-waiting true])
            :loop           (= @(rf/subscribe [:player/loop]) :stream)
            :muted          @(rf/subscribe [:player/muted])
            :on-can-play    #(rf/dispatch [:bg-player/set-ready true])
            :on-seeked      #(reset! !elapsed (.-currentTime @!player))
            :on-progress    #(on-progress !player !buffered)
            :on-time-update #(on-update !player !buffered !elapsed)
            :on-loaded-data #(rf/dispatch [:bg-player/start])
            :on-play        #(rf/dispatch [:bg-player/play !player stream])
            :on-error       #(rf/dispatch [:player/on-error !player])
            :on-pause       #(rf/dispatch [:bg-player/set-paused true])}]))})))

(defn player
  []
  (let [!player       @(rf/subscribe [:bg-player])
        !elapsed-time @(rf/subscribe [:elapsed-time])
        stream        @(rf/subscribe [:queue/current])
        show-queue?   @(rf/subscribe [:queue/show])
        show-player?  @(rf/subscribe [:bg-player/show])
        color         (-> stream
                          :service-id
                          utils/get-service-color)]
    [:<>
     (when show-player?
       [audio-player !player])
     [:> AnimatePresence
      (when (and show-player? (not show-queue?))
        [:> (.-div motion)
         {:animate    {:y 0}
          :initial    {:y 100}
          :transition {:ease "easeOut" :duration 0.3}
          :exit       {:y 100}
          :class      ["h-[80px]" "sticky" "flex" "items-center" "left-0"
                       "right-0" "bottom-0" "z-10" "relative"
                       "cursor-pointer" "bg-neutral-200" "dark:bg-neutral-900"]
          :on-click   #(rf/dispatch [:queue/show true])}
         [:div.flex.flex-col.w-full
          [:div.absolute.top-0.left-0.w-full.lg:hidden.flex
           [time-slider !player !elapsed-time :height "0.25rem" :thumb-size 0
            :progress-color color]]
          [:div.flex.items-center.px-3
           [metadata stream]
           [main-controls !player color]
           [extra-controls !player color]]]])]]))
