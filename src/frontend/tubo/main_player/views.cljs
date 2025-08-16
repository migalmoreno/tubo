(ns tubo.main-player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.bg-player.views :as bg-player]
   [tubo.player.views :as player]
   [tubo.queue.views :as queue]
   [tubo.stream.views :as stream]
   [tubo.utils :as utils]))

(defn player-args
  [!player]
  (let [!elapsed @(rf/subscribe [:elapsed-time])]
    {:muted          @(rf/subscribe [:player/muted])
     :on-can-play    #(rf/dispatch [:main-player/ready true])
     :on-play        #(rf/dispatch [:main-player/play])
     :on-loaded-data #(rf/dispatch [:main-player/start])
     :on-time-update #(when @!player
                        (reset! !elapsed (.-currentTime @!player)))
     :on-seeked      #(when @!player
                        (reset! !elapsed (.-currentTime @!player)))
     :loop           (= @(rf/subscribe [:player/loop]) :stream)}))

(defn stream-queue
  [stream pos]
  (let [queue         @(rf/subscribe [:queue])
        bookmarks     @(rf/subscribe [:bookmarks])
        loop-playback @(rf/subscribe [:player/loop])
        color         (utils/get-service-color (:service-id stream))
        shuffled      @(rf/subscribe [:player/shuffled])]
    [:div.flex.flex-col.w-full
     [:div.bg-neutral-200.dark:bg-neutral-950.rounded-t-lg
      [:div.p-5.flex.items-center.justify-between
       [:div.flex.flex-col
        [:h4.font-bold.text-lg "Queue"]
        [:span.text-xs.text-neutral-600.dark:text-neutral-500
         (str (inc pos) "/" (count queue))]]
       [:div.flex.items-center
        [:div.px-4
         [player/loop-button loop-playback color true]]
        [:div.pl-4.pr-5
         [player/shuffle-button shuffled color true]]
        [bg-player/popover stream]]]
      [:div.flex.flex-col.gap-y-1.w-full.h-fit.max-h-64.overflow-y-auto.relative.scroll-smooth
       (for [[i item] (map-indexed vector queue)]
         ^{:key i} [queue/queue-item item queue pos i bookmarks])]]]))

(defn player
  []
  (let [pos          @(rf/subscribe [:queue/position])
        !player      @(rf/subscribe [:main-player])
        stream       @(rf/subscribe [:queue/current])
        show-player? @(rf/subscribe [:main-player/show])]
    [:div.fixed.w-full.overflow-auto.z-10.transition-all.ease-in-out.justify-center.flex.left-0.backdrop-blur
     {:class ["h-[calc(100%-56px)]" "bg-neutral-100/90" "dark:bg-neutral-950/90"
              (if show-player? "translate-y-0" "translate-y-full")]}
     (when (and show-player? stream)
       [:div.flex.flex-col.flex-auto.items-center.md:my-4.relative
        [:div.flex.gap-x-6.w-full.flex-wrap {:class ["md:w-[95%] xl:w-11/12"]}
         [:div.flex.flex-col.w-full.flex-1
          [:div.flex.flex-col.justify-center.items-center.sticky.md:static.z-10
           [player/video-player stream !player (player-args !player)
            #(rf/dispatch [:main-player/set-stream stream pos])]]
          [:div.flex.flex-col.py-4.w-full.px-4.md:px-0
           [stream/metadata stream]
           [:div.hidden.lg:flex.flex-col.gap-y-8.py-4
            [stream/description stream]]
           [:div.flex.md:hidden.flex-col.gap-y-8
            [stream/description stream]]]]
         [:div.w-full.py-4.lg:py-0 {:class "lg:max-w-[450px]"}
          [stream-queue stream pos]]]])]))
