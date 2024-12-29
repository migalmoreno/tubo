(ns tubo.main-player.views
  (:require
   [re-frame.core :as rf]
   [tubo.bg-player.views :as bg-player]
   [tubo.layout.views :as layout]
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
     :on-time-update #(reset! !elapsed (.-currentTime @!player))
     :on-seeked      #(reset! !elapsed (.-currentTime @!player))
     :loop           (= @(rf/subscribe [:player/loop]) :stream)}))

(defn player
  []
  (let [queue         @(rf/subscribe [:queue])
        pos           @(rf/subscribe [:queue/position])
        bookmarks     @(rf/subscribe [:bookmarks])
        !player       @(rf/subscribe [:main-player])
        stream        @(rf/subscribe [:queue/current])
        show-player?  @(rf/subscribe [:main-player/show])
        loop-playback @(rf/subscribe [:player/loop])
        color         (-> stream
                          :service-id
                          utils/get-service-color)
        shuffled      @(rf/subscribe [:player/shuffled])]
    [:div.fixed.w-full.bg-neutral-100.dark:bg-neutral-900.overflow-auto.z-10.transition-all.ease-in-out
     {:class ["h-[calc(100%-56px)]"
              (if show-player? "translate-y-0" "translate-y-full")]}
     (when (and show-player? stream)
       [:div.flex.flex-col.items-center.justify-center
        [:div.flex.flex-col.items-center.w-full.xl:py-6
         [player/video-player stream !player (player-args !player)
          #(rf/dispatch [:main-player/set-stream stream pos])]]
        [:div.flex.flex-col.w-full.p-4
         {:class ["lg:w-4/5" "xl:w-3/5"]}
         [:div.bg-neutral-200.dark:bg-neutral-950
          [:div.p-5.flex.items-center.justify-between.rounded-t-md
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
             ^{:key i} [queue/queue-item item queue pos i bookmarks])]]]
        [:div.w-full
         [layout/content-container
          [stream/metadata stream]
          [stream/description stream]
          [stream/comments stream]
          [stream/related-items stream]]]])]))
