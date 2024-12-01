(ns tubo.main-player.views
  (:require
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.queue.views :as queue]
   [tubo.stream.views :as stream]))

(defn player
  []
  (let [queue        @(rf/subscribe [:queue])
        queue-pos    @(rf/subscribe [:queue/position])
        bookmarks    @(rf/subscribe [:bookmarks])
        !player      @(rf/subscribe [:main-player])
        stream       @(rf/subscribe [:queue/current])
        show-player? @(rf/subscribe [:main-player/show])]
    [:div.fixed.w-full.bg-neutral-100.dark:bg-neutral-900.overflow-auto.z-10.transition-all.ease-in-out
     {:class ["h-[calc(100%-56px)]"
              (if show-player? "translate-y-0" "translate-y-full")]}
     (when (and show-player? stream)
       [:div
        [:div.flex.flex-col.items-center.w-full.xl:py-6
         [player/video-player stream !player]]
        [:div.flex.items-center.justify-center
         [:div.flex.flex-col.gap-y-1.w-full.h-fit.max-h-64.overflow-y-auto
          {:class ["lg:w-4/5" "xl:w-3/5"]}
          (for [[i item] (map-indexed vector queue)]
            ^{:key i} [queue/queue-item item queue queue-pos i bookmarks])]]
        [layout/content-container
         [stream/metadata stream]
         [stream/description stream]
         [stream/comments stream]
         [stream/related-items stream]]])]))
