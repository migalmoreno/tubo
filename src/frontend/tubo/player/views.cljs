(ns tubo.player.views
  (:require
   ["motion/react" :refer [motion AnimatePresence]]
   [re-frame.core :as rf]
   [tubo.player.components :as player]
   [tubo.stream.views :as stream]
   [tubo.ui :as ui]
   [tubo.utils :as utils]))

(defn metadata
  [{:keys [name uploader-name] :as stream}]
  [:div.flex.lg:flex-1.group
   [:div.flex.gap-x-4
    [:div
     [ui/thumbnail (dissoc stream :duration) nil :container-classes
      ["h-12" "w-12"] :image-classes ["rounded"]]]
    [:div.flex.flex-col.pr-4.gap-y-1
     [:h1.text-sm.line-clamp-1.w-fit
      {:title name}
      name]
     [:h1.text-xs.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit
      {:title uploader-name}
      uploader-name]]]
   (when stream
     [:div.invisible.group-hover:visible
      [player/popover stream :tooltip-classes ["bottom-7" "left-0"]]])])

(defn main-controls
  [!player color]
  (let [queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        !paused          @(rf/subscribe [:player/paused])
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
       (if (and (not waiting?) (or (nil? bg-player-ready?) @!player))
         (if @!paused
           [:i.fa-solid.fa-play-circle]
           [:i.fa-solid.fa-pause-circle])
         [ui/loading-icon color "text-3xl lg:text-4xl"])
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
       [player/time-slider !player !elapsed-time :progress-color color :rounded?
        true
        :thumb-size "0.5rem" :thumb-color color]]
      [:span.w-16.flex.justify-start
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]]))

(defn extra-controls
  [!player color]
  (let [muted?     @(rf/subscribe [:player/muted])
        dark-theme @(rf/subscribe [:dark-theme])]
    [:div.flex.lg:justify-end.lg:flex-1.gap-x-4
     [:div.hidden.lg:flex.w-36.items-center.gap-x-4
      {:style {"--thumb-bg"
               (if dark-theme "rgb(212,212,212)" "rgb(0,0,0)")}}
      [player/button
       :icon
       (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
       :on-click #(rf/dispatch [:bg-player/mute (not muted?) !player])
       :show-on-mobile? true
       :extra-classes ["w-14"]]
      [player/volume-slider !player :progress-color color :height "0.375rem"
       :thumb-size
       "0.375rem" :thumb-color color :rounded? true]]
     [player/button
      :icon [:i.fa-solid.fa-up-right-and-down-left-from-center]
      :on-click #(rf/dispatch [:queue/show true])
      :show-on-mobile? true
      :extra-classes
      ["text-lg" "lg:text-base" "hidden" "lg:block"]]]))

(defn bg-player
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
       [player/audio-player !player])
     [:> AnimatePresence
      (when (and show-player? (not show-queue?))
        [:> (.-div motion)
         {:animate    {:y 0}
          :initial    {:y 100}
          :transition {:ease "easeOut" :duration 0.3}
          :exit       {:y 100}
          :class      ["h-[80px]" "sticky" "flex" "items-center" "left-0"
                       "right-0" "bottom-0" "z-10" "relative" "cursor-pointer"
                       "bg-neutral-200" "dark:bg-neutral-900"]
          :on-click   #(rf/dispatch [:queue/show true])}
         [:div.flex.flex-col.w-full
          [:div.absolute.top-0.left-0.w-full.lg:hidden.flex
           [player/time-slider !player !elapsed-time :height "0.25rem"
            :thumb-size 0
            :progress-color color]]
          [:div.flex.items-center.px-3
           [metadata stream]
           [main-controls !player color]
           [extra-controls !player color]]]])]]))

(defn main-player
  []
  (let [pos      @(rf/subscribe [:queue/position])
        !player  @(rf/subscribe [:main-player])
        stream   @(rf/subscribe [:queue/current])
        !elapsed @(rf/subscribe [:elapsed-time])]
    [:div.relative.overflow-auto.w-full.h-full
     [stream/stream-container stream
      [stream/video-container stream
       [player/video-player stream !player
        {:muted          @(rf/subscribe [:player/muted])
         :on-can-play    #(rf/dispatch [:main-player/ready true])
         :on-play        #(rf/dispatch [:main-player/play])
         :on-time-update #(when @!player
                            (reset! !elapsed (.-currentTime @!player)))
         :on-seeked      #(when @!player
                            (reset! !elapsed (.-currentTime @!player)))
         :loop           (= @(rf/subscribe [:player/loop]) :stream)}
        #(rf/dispatch [:main-player/mount stream !player pos])
        #(rf/dispatch [:main-player/unmount])]]]]))
