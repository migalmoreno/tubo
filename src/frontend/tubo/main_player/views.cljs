(ns tubo.main-player.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.player.views :as player]
   [tubo.stream.views :as stream]))

(defn player-args
  [!player !playing?]
  (let [!elapsed @(rf/subscribe [:elapsed-time])]
    {:muted          @(rf/subscribe [:player/muted])
     :on-can-play    #(rf/dispatch [:main-player/ready true])
     :on-play        #(rf/dispatch [:main-player/play])
     :on-loaded-data #(rf/dispatch [:main-player/start])
     :on-playing     #(reset! !playing? true)
     :on-time-update #(when (and @!player @!playing?)
                        (reset! !elapsed (.-currentTime @!player)))
     :on-seeked      #(when @!player
                        (reset! !elapsed (.-currentTime @!player)))
     :loop           (= @(rf/subscribe [:player/loop]) :stream)}))

(defn player
  []
  (let [!playing? (r/atom false)]
    (fn []
      (let [pos          @(rf/subscribe [:queue/position])
            !player      @(rf/subscribe [:main-player])
            stream       @(rf/subscribe [:queue/current])
            show-player? @(rf/subscribe [:main-player/show])]
        [:> AnimatePresence
         (when show-player?
           [:> motion.div
            {:class   ["w-full" "z-10" "justify-center" "flex" "left-0"
                       "backdrop-blur" "fixed" "min-h-[calc(100dvh-56px)]"
                       "max-h-[calc(100dvh-56px)]"
                       "bg-neutral-100/90" "dark:bg-neutral-950/90"]
             :animate {:transform "translateY(0)"}
             :initial {:transform "translateY(100%)"}
             :exit    {:transform "translateY(100%)"}}
            [:div.relative.overflow-auto.w-full
             [stream/stream stream
              [stream/video-container stream
               [player/video-player stream !player
                (player-args !player !playing?)
                #(rf/dispatch [:player/initialize stream !player pos])
                #(reset! !playing? false)]]]]])]))))
