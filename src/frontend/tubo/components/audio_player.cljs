(ns tubo.components.audio-player
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.loading :as loading]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn player
  []
  (let [!player (r/atom nil)
        !elapsed-time (r/atom 0)
        !autoplay? (r/atom true)
        !volume-level (r/atom 100)]
    (fn []
      (let [media-queue @(rf/subscribe [:media-queue])
            media-queue-pos @(rf/subscribe [:media-queue-pos])
            {:keys [uploader-name uploader-url
                    name stream url service-color] :as current-stream} @(rf/subscribe [:media-queue-stream])
            show-audio-player? @(rf/subscribe [:show-audio-player])
            show-audio-player-loading? @(rf/subscribe [:show-audio-player-loading])
            is-window-visible @(rf/subscribe [:is-window-visible])
            loop-file? @(rf/subscribe [:loop-file])
            loop-playlist? @(rf/subscribe [:loop-playlist])]
        (when show-audio-player?
          [:div.sticky.bottom-0.z-50.bg-white.dark:bg-neutral-900.px-3.py-5.sm:p-5.absolute.box-border.m-0
           {:style {:borderColor service-color :borderTopWidth "2px" :borderStyle "solid"}}
           [:div.flex.items-center.justify-between
            [:div.flex.items-center
             [:div.flex.flex-col
              [:a.text-xs.line-clamp-1
               {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
              [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
             [:audio
              {:src                stream :ref #(reset! !player %)
               :loop               loop-file?
               :on-time-update     #(when (and @!player (> (.-readyState @!player) 0))
                                      (reset! !elapsed-time (.-currentTime @!player)))
               :on-loaded-data #(when (and @!player (> (.-readyState @!player) 0))
                                  (.play @!player)
                                  (set! (.-currentTime @!player) @!elapsed-time))
               :on-ended           #(when (and @!player (> (.-readyState @!player) 0))
                                      (let [idx (if (< (+ media-queue-pos 1) (count media-queue))
                                                  (+ media-queue-pos 1)
                                                  (if loop-playlist? 0 media-queue-pos))]
                                        (rf/dispatch [::events/change-media-queue-pos idx])
                                        (reset! !elapsed-time 0)
                                        (when (and (not is-window-visible) loop-playlist?)
                                          (set! (.-src @!player) (:stream (nth media-queue idx)))
                                          (.play @!player))))}]]
            [:div.flex
             [:button.focus:outline-none.mx-1.sm:mx-2
              {:class    (when-not (and media-queue (not= media-queue-pos 0))
                           "opacity-50 cursor-auto")
               :on-click (when (and media-queue (not= media-queue-pos 0))
                           #(do
                              (rf/dispatch [::events/change-media-queue-pos
                                            (- media-queue-pos 1)])
                              (reset! !elapsed-time 0)))}
              [:i.fa-solid.fa-backward-step]]
             [:button.focus:outline-none.mx-1.sm:mx-2
              {:on-click #(when-let [player @!player]
                            (if (.-paused player)
                              (.play player)
                              (.pause player)))}
              (if @!player
                (if show-audio-player-loading?
                  [loading/loading-icon service-color "text-1xl"]
                  (if (.-paused @!player)
                    [:i.fa-solid.fa-play]
                    [:i.fa-solid.fa-pause]))
                [:i.fa-solid.fa-play])]
             [:button.focus:ring-transparent.mx-1.sm:mx-2
              {:class    (when-not (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                           "opacity-50 cursor-auto")
               :on-click (when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                           #(do
                              (rf/dispatch [::events/change-media-queue-pos
                                            (+ media-queue-pos 1)])
                              (reset! !elapsed-time 0)))}
              [:i.fa-solid.fa-forward-step]]
             [:div.flex
              [:div.mx-2.hidden.sm:flex
               [:span (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
               [:span.mx-2 "/"]
               [:span (if (and @!player (> (.-readyState @!player) 0))
                        (util/format-duration (.-duration @!player))
                        "00:00")]]
              [:input.mx-2.w-20.ml:w-80.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none
               {:type      "range"
                :on-input  #(reset! !elapsed-time (.. % -target -value))
                :on-change #(and @!player (> (.-readyState @!player) 0)
                                 (set! (.-currentTime @!player) @!elapsed-time))
                :style     {:accentColor service-color}
                :max       (if (and @!player (> (.-readyState @!player) 0))
                             (.floor js/Math (.-duration @!player))
                             100)
                :value     @!elapsed-time}]
              [:button.focus:ring-transparent.mx-1.sm:mx-2
               {:on-click #(rf/dispatch [::events/toggle-loop-file])}
               [:i.fa-solid.fa-repeat
                {:style {:color (when loop-file? service-color)}}]]
              [:button.focus:ring-transparent.mx-1.sm:mx-2
               {:on-click #(rf/dispatch [::events/toggle-loop-playlist])}
               [:i.fa-solid.fa-retweet
                {:style {:color (when loop-playlist? service-color)}}]]
              [:div.hidden.sm:flex
               [:button.focus:outline-none.mx-1.sm:mx-2
                {:on-click #(when-let [player @!player]
                              (set! (.-muted player) (not (.-muted player))))}
                (if (and @!player (.-muted @!player))
                  [:i.fa-solid.fa-volume-xmark]
                  [:i.fa-solid.fa-volume-low])]
               [:input.w-20.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none
                {:type "range"
                 :on-input #(do (reset! !volume-level (.. % -target -value))
                                (and @!player (> (.-readyState @!player) 0)
                                     (set! (.-volume @!player) (/ @!volume-level 100))))
                 :style {:accentColor service-color}
                 :max 100
                 :value @!volume-level}]]]
             [:div.mx-1.sm:mx-2
              [:i.fa-solid.fa-close.cursor-pointer
               {:on-click (fn []
                            (rf/dispatch [::events/toggle-audio-player])
                            (.pause @!player))}]]]]])))))
