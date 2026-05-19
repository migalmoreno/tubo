(ns tubo.player.views
  (:require
   ["motion/react" :refer [motion AnimatePresence]]
   [nano-id.core :refer [nano-id]]
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
  [color !player]
  (let [!elapsed   @(rf/subscribe [:elapsed-time])
        !buffered  @(rf/subscribe [:player/buffered])
        !duration  @(rf/subscribe [:player/duration])
        dark-theme @(rf/subscribe [:dark-theme])]
    [:div.flex.flex-col.items-center.ml-auto.gap-y-2
     [:div.flex.justify-end.gap-x-4.items-center
      [player/loop-button color false :extra-classes ["text-sm"]]
      [player/prev-track-button !player]
      [player/seek-backward-button !player !elapsed]
      [player/play-button !player color
       :loading-extra-classes ["text-3xl" "lg:text-4xl"]
       :button-extra-classes
       ["text-3xl" "lg:text-4xl" "w-[3rem]" "lg:w-[2.5rem]" "!p-0"
        "!bg-transparent"]]
      [player/seek-forward-button !player !elapsed]
      [player/next-track-button]
      [player/shuffle-button color false :extra-classes ["text-sm"]]]
     [:div.hidden.lg:flex.items-center.gap-x-2
      {:class "text-[0.8rem]"}
      [player/elapsed-time !player !elapsed :extra-classes ["justify-end"]]
      [:div.w-20.lg:w-96.mx-2.flex.items-center
       {:style {"--thumb-bg" (if dark-theme
                               "rgb(212,212,212)"
                               "rgb(0,0,0)")}}
       [player/time-slider !player !elapsed !buffered
        :progress-color color :rounded? true
        :thumb-size "0.5rem" :thumb-color color]]
      [player/duration-time @!duration :extra-classes ["justify-start"]]]]))

(defn extra-controls
  [color !player]
  (let [muted?     @(rf/subscribe [:player/muted])
        dark-theme @(rf/subscribe [:dark-theme])]
    [:div.flex.lg:justify-end.lg:flex-1.gap-x-4
     [:div.hidden.lg:flex.w-36.items-center.gap-x-4
      {:style {"--thumb-bg"
               (if dark-theme "rgb(212,212,212)" "rgb(0,0,0)")}}
      [player/button
       :icon
       (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
       :on-click #(rf/dispatch [:player/mute !player (not muted?)])
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
  (let [id (nano-id)]
    (fn []
      (let [!player      @(rf/subscribe [:player-by-id id])
            !elapsed     @(rf/subscribe [:elapsed-time])
            !buffered    @(rf/subscribe [:player/buffered])
            stream       @(rf/subscribe [:queue/current])
            pos          @(rf/subscribe [:queue/position])
            show-queue?  @(rf/subscribe [:queue/show])
            show-player? @(rf/subscribe [:bg-player/show])
            color        (utils/get-service-color (:service-id stream))]
        [:<>
         (when stream
           [player/audio-player
            #(rf/dispatch [:bg-player/mount id % stream pos])
            #(rf/dispatch [:bg-player/unmount id])])
         [:> AnimatePresence
          (when (and show-player? (not show-queue?))
            [:> (.-div motion)
             {:animate    {:y 0}
              :initial    {:y 100}
              :transition {:ease "easeOut" :duration 0.3}
              :exit       {:y 100}
              :class      ["h-[80px]" "sticky" "flex" "items-center" "left-0"
                           "right-0" "bottom-0" "z-10" "relative"
                           "cursor-pointer"
                           "bg-neutral-200" "dark:bg-neutral-900"]
              :on-click   #(rf/dispatch [:queue/show true])}
             [:div.flex.flex-col.w-full
              [:div.absolute.top-0.left-0.w-full.lg:hidden.flex
               [player/time-slider !player !elapsed !buffered
                :height "0.25rem" :thumb-size 0 :progress-color color]]
              [:div.flex.items-center.px-3
               [metadata stream]
               [main-controls color !player]
               [extra-controls color !player]]]])]]))))

(defn main-player
  [bg-player-id]
  (let [stream       @(rf/subscribe [:queue/current])
        embed-player @(rf/subscribe [:player-by-id bg-player-id])]
    [:div.relative.overflow-auto.w-full.h-full
     [stream/stream-container stream
      [stream/video-container stream
       [player/video-player stream
        #(rf/dispatch [:main-player/mount stream])
        #(rf/dispatch [:main-player/unmount])
        embed-player]]]]))
