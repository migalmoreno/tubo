(ns tubo.components.play-queue
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn queue
  []
  (let [show-media-queue @(rf/subscribe [:show-media-queue])
        media-queue @(rf/subscribe [:media-queue])
        media-queue-pos @(rf/subscribe [:media-queue-pos])
        {:keys [uploader-name uploader-url
                name stream url service-color] :as current-stream} @(rf/subscribe [:media-queue-stream])
        !elapsed-time @(rf/subscribe [:elapsed-time])
        !player @(rf/subscribe [:player])
        loop-file? @(rf/subscribe [:loop-file])
        loop-playlist? @(rf/subscribe [:loop-playlist])]
    (when (and show-media-queue media-queue)
      [:div.fixed.flex.flex-col.items-center.px-5.py-2.min-w-full.w-full.z-30
       {:style {:minHeight "calc(100vh - 56px)" :height "calc(100vh - 56px)"}
        :class "dark:bg-neutral-900/90 bg-white/90 backdrop-blur"}
       [:div.flex.justify-between.pl-4.items-center.w-full.shrink-0
        {:class "ml:w-4/5 xl:w-3/5"}
        [:h1.text-2xl.font-bold.py-6 "Play Queue"]
        [:div.mx-2
         [:i.fa-solid.fa-close.cursor-pointer
          {:on-click #(rf/dispatch [::events/toggle-media-queue])}]]]
       [:div.flex.flex-col.sm:p-4.w-full.overflow-y-auto.flex-auto
        {:class "ml:w-4/5 xl:w-3/5"}
        (for [[i {:keys [uploader-name uploader-url name duration
                         stream url service-color thumbnail-url]}] (map-indexed vector media-queue)]
          (let [service-name (case service-color
                               "#cc0000" "YouTube"
                               "#ff7700" "SoundCloud"
                               "#333333" "media.ccc.de"
                               "#F2690D" "PeerTube"
                               "#629aa9" "Bandcamp")]
            [:div.flex.w-full.h-24.rounded.px-2.cursor-pointer.my-2
             {:key      i
              :class    (when (= i media-queue-pos) "bg-[#f0f0f0] dark:bg-stone-800")
              :on-click #(do
                           (rf/dispatch [::events/change-media-queue-pos i])
                           (reset! !elapsed-time 0))}
             [:div.w-56
              [items/thumbnail thumbnail-url nil url name duration {:classes "h-24"}]]
             [:div.flex.flex-col.px-4.py-2.w-full
              [:h1.line-clamp-1 name]
              [:div.text-neutral-600.dark:text-neutral-300.text-sm.flex.flex-col.xs:flex-row
               [:span.line-clamp-1 uploader-name]
               [:span.px-2.hidden.xs:inline-block {:dangerouslySetInnerHTML {:__html "&bull;"}}]
               [:span service-name]]]]))]
       [:div.flex.flex-col.p-4.w-full.shrink-0
        {:class "ml:w-4/5 xl:w-3/5"}
        [:div.flex.flex-col.w-full.py-2
         [:a.text-md.line-clamp-1
          {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
         [:a.text-sm.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
          {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
        [:div.flex.flex-auto.py-2.w-full.items-center
         [:span (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
         [:input.mx-2.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.w-full.h-1
          {:type      "range"
           :on-input  #(reset! !elapsed-time (.. % -target -value))
           :on-change #(and @!player (> (.-readyState @!player) 0)
                            (set! (.-currentTime @!player) @!elapsed-time))
           :style     {:accentColor service-color}
           :max       (if (and @!player (> (.-readyState @!player) 0))
                        (.floor js/Math (.-duration @!player))
                        100)
           :value     @!elapsed-time}]
         [:span (if (and @!player (> (.-readyState @!player) 0))
                  (util/format-duration (.-duration @!player))
                  "00:00")]]
        [:div.flex.justify-center.items-center
         [:button.focus:ring-transparent.mx-2
               {:on-click #(rf/dispatch [::events/toggle-loop-file])}
               [:i.fa-solid.fa-repeat
                {:style {:color (when loop-file? service-color)}}]]
         [:button.focus:outline-none.mx-2.text-xl
          {:class    (when-not (and media-queue (not= media-queue-pos 0))
                       "opacity-50 cursor-auto")
           :on-click (when (and media-queue (not= media-queue-pos 0))
                       #(do
                          (rf/dispatch [::events/change-media-queue-pos
                                        (- media-queue-pos 1)])
                          (reset! !elapsed-time 0)))}
          [:i.fa-solid.fa-backward-step]]
         [:button.focus:outline-none.mx-2.text-xl
          {:on-click #(set! (.-currentTime @!player) (- @!elapsed-time 5))}
          [:i.fa-solid.fa-backward]]
         [:button.focus:outline-none.mx-2.text-3xl
          {:on-click #(when-let [player @!player]
                        (if (.-paused player)
                          (.play player)
                          (.pause player)))}
          (if (and @!player (.-paused @!player))
            [:i.fa-solid.fa-play]
            [:i.fa-solid.fa-pause])]
         [:button.focus:outline-none.mx-2.text-xl
          {:on-click #(set! (.-currentTime @!player) (+ @!elapsed-time 5))}
          [:i.fa-solid.fa-forward]]
         [:button.focus:ring-transparent.mx-2.text-xl
          {:class    (when-not (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                       "opacity-50 cursor-auto")
           :on-click (when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
                       #(do
                          (rf/dispatch [::events/change-media-queue-pos
                                        (+ media-queue-pos 1)])
                          (reset! !elapsed-time 0)))}
          [:i.fa-solid.fa-forward-step]]
         [:button.focus:ring-transparent.mx-2
          {:on-click #(rf/dispatch [::events/toggle-loop-playlist])}
          [:i.fa-solid.fa-retweet
           {:style {:color (when loop-playlist? service-color)}}]]]]])))
