(ns tubo.main-player.views
  (:require
   [re-frame.core :as rf]
   [tubo.player.views :as player]
   [tubo.stream.views :as stream]))

(defn player-container
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
        #(rf/dispatch [:main-player/initialize stream !player pos])
        #(rf/dispatch [:main-player/unmount])]]]]))
