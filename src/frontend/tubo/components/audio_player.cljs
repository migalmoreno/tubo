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
  (let [!autoplay? (r/atom true)]
    (fn []
      (let [media-queue @(rf/subscribe [:media-queue])
            media-queue-pos @(rf/subscribe [:media-queue-pos])
            {:keys [uploader-name uploader-url thumbnail-url
                    name stream url service-color] :as current-stream} @(rf/subscribe [:media-queue-stream])
            show-audio-player? @(rf/subscribe [:show-audio-player])
            show-audio-player-loading? @(rf/subscribe [:show-audio-player-loading])
            show-media-queue? @(rf/subscribe [:show-media-queue])
            is-window-visible @(rf/subscribe [:is-window-visible])
            loop-file? @(rf/subscribe [:loop-file])
            loop-playlist? @(rf/subscribe [:loop-playlist])
            volume-level @(rf/subscribe [:volume-level])
            muted? @(rf/subscribe [:muted])
            !elapsed-time @(rf/subscribe [:elapsed-time])
            !player @(rf/subscribe [:player])]
        (when show-audio-player?
          [:div.sticky.bottom-0.z-40.bg-white.dark:bg-neutral-900.p-3.sm:p-5.absolute.box-border.m-0
           {:style {:borderTop (str "2px solid " service-color) :display (when show-media-queue? "none")}}
           [:div.flex.items-center.justify-between
            [:div.flex.items-center
             [:div {:style {:height "40px" :width "70px" :maxWidth "70px" :minWidth "70px"}}
              [:img.min-h-full.max-h-full.object-cover.min-w-full.max-w-full.w-full {:src thumbnail-url}]]
             [:div.flex.flex-col.px-2
              [:a.text-xs.line-clamp-1
               {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
              [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
             [:audio
              {:src                stream
               :ref #(reset! !player %)
               :loop               loop-file?
               :on-time-update     #(when (and @!player (> (.-readyState @!player) 0))
                                      (reset! !elapsed-time (.-currentTime @!player)))
               :on-loaded-data #(when (and @!player (> (.-readyState @!player) 0))
                                  (rf/dispatch [::events/start-playback @!player])
                                  (set! (.-currentTime @!player) @!elapsed-time))
               :on-ended           #(when (and @!player (> (.-readyState @!player) 0))
                                      (let [idx (if (< (+ media-queue-pos 1) (count media-queue))
                                                  (+ media-queue-pos 1)
                                                  (if loop-playlist? 0 media-queue-pos))]
                                        (rf/dispatch [::events/change-media-queue-pos idx])
                                        (reset! !elapsed-time 0)
                                        (when (and (not is-window-visible) loop-playlist?)
                                          (set! (.-src @!player) (:stream (nth media-queue idx)))
                                          (rf/dispatch [::events/start-playback @!player]))))}]]
            [:div.flex
             [:button:focus:ring-transparent.mx-2.cursor-pointer
               {:on-click #(rf/dispatch [::events/toggle-media-queue])}
               [:i.fa-solid.fa-list]]
             [:button.hidden.ml:block.focus:outline-none.mx-2
              {:class    (when-not (and media-queue (not= media-queue-pos 0))
                           "opacity-50 cursor-auto")
               :on-click (when (and media-queue (not= media-queue-pos 0))
                           #(do
                              (rf/dispatch [::events/change-media-queue-pos
                                            (- media-queue-pos 1)])
                              (reset! !elapsed-time 0)))}
              [:i.fa-solid.fa-backward-step]]
             [:button.hidden.ml:block.focus:outline-none.mx-2
              {:on-click #(set! (.-currentTime @!player) (- @!elapsed-time 5))}
              [:i.fa-solid.fa-backward]]
             [:button.focus:outline-none.mx-2
              {:on-click #(rf/dispatch [::events/start-playback @!player])}
              (if @!player
                (if show-audio-player-loading?
                  [loading/loading-icon service-color "text-1xl"]
                  (if (.-paused @!player)
                    [:i.fa-solid.fa-play]
                    [:i.fa-solid.fa-pause]))
                [:i.fa-solid.fa-play])]
             [:button.hidden.ml:block.focus:outline-none.mx-2
              {:on-click #(set! (.-currentTime @!player) (+ @!elapsed-time 5))}
              [:i.fa-solid.fa-forward]]
             [:button.hidden.ml:block.focus:ring-transparent.mx-2
              {:class    (when-not (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                           "opacity-50 cursor-auto")
               :on-click (when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                           #(do
                              (rf/dispatch [::events/change-media-queue-pos
                                            (+ media-queue-pos 1)])
                              (reset! !elapsed-time 0)))}
              [:i.fa-solid.fa-forward-step]]
             [:div.flex.items-center
              [:span.hidden.ml:block.mx-2 (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
              [:input.hidden.ml:block.mx-2.w-20.ml:w-56.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.h-1
               {:type      "range"
                :on-input  #(reset! !elapsed-time (.. % -target -value))
                :on-change #(and @!player (> (.-readyState @!player) 0)
                                 (set! (.-currentTime @!player) @!elapsed-time))
                :style     {:accentColor service-color}
                :max       (if (and @!player (> (.-readyState @!player) 0))
                             (.floor js/Math (.-duration @!player))
                             100)
                :value     @!elapsed-time}]
              [:span.hidden.ml:block.mx-2 (if (and @!player (> (.-readyState @!player) 0))
                            (util/format-duration (.-duration @!player))
                            "00:00")]
              [:button.hidden.ml:flex.focus:ring-transparent.mx-2
               {:on-click #(rf/dispatch [::events/toggle-loop-file])}
               [:i.fa-solid.fa-repeat
                {:style {:color (when loop-file? service-color)}}]]
              [:button.hidden.ml:flex.focus:ring-transparent.mx-2
               {:on-click #(rf/dispatch [::events/toggle-loop-playlist])}
               [:i.fa-solid.fa-retweet
                {:style {:color (when loop-playlist? service-color)}}]]
              [:div.hidden.ml:flex.items-center
               [:button.focus:outline-none.mx-2
                {:on-click #(rf/dispatch [::events/toggle-mute @!player])}
                (if (or (and @!player muted?))
                  [:i.fa-solid.fa-volume-xmark]
                  [:i.fa-solid.fa-volume-low])]
               [:input.w-20.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.h-1.range-sm.mx-2
                {:type "range"
                 :on-input #(rf/dispatch [::events/change-volume-level (.. % -target -value) @!player])
                 :style {:accentColor service-color}
                 :max 100
                 :value volume-level}]]]
             [:div.mx-2
              [:i.fa-solid.fa-close.cursor-pointer
               {:on-click (fn []
                            (rf/dispatch [::events/toggle-audio-player])
                            (.pause @!player))}]]]]])))))
