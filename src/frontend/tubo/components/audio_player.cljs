(ns tubo.components.audio-player
  (:require
   [goog.object :as gobj]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.loading :as loading]
   [tubo.components.player :as player]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn audio-source
  []
  (let [{:keys [stream]} @(rf/subscribe [:media-queue-stream])]
    (r/create-class
     {:display-name "AudioPlayer"
      :component-did-mount
      (fn [this]
        (when stream
          (set! (.-src (rdom/dom-node this)) stream)))
      :reagent-render
      (fn []
        (let [!player       @(rf/subscribe [:player])
              !elapsed-time @(rf/subscribe [:elapsed-time])
              player-ready? (and @!player (> (.-readyState @!player) 0))
              muted?        @(rf/subscribe [:muted])
              volume-level  @(rf/subscribe [:volume-level])
              loop-playback @(rf/subscribe [:loop-playback])]
          [:audio
           {:ref            #(reset! !player %)
            :loop           (= loop-playback :stream)
            :on-loaded-data #(do (when (.-paused @!player)
                                   (.play @!player))
                                 (when (= (.-currentTime @!player) 0)
                                   (set! (.-currentTime @!player) @!elapsed-time))
                                 (set! (.-volume @!player) (/ volume-level 100)))
            :muted          muted?
            :on-time-update #(when player-ready?
                               (reset! !elapsed-time (.-currentTime @!player)))}]))})))

(defn player
  []
  (let [media-queue                @(rf/subscribe [:media-queue])
        media-queue-pos            @(rf/subscribe [:media-queue-pos])
        {:keys
         [uploader-name uploader-url thumbnail-url
          name stream url service-color] :as current-stream}
        @(rf/subscribe [:media-queue-stream])
        show-audio-player?         @(rf/subscribe [:show-audio-player])
        show-audio-player-loading? @(rf/subscribe [:show-audio-player-loading])
        show-media-queue?          @(rf/subscribe [:show-media-queue])
        loop-playback              @(rf/subscribe [:loop-playback])
        volume-level               @(rf/subscribe [:volume-level])
        muted?                     @(rf/subscribe [:muted])
        !elapsed-time              @(rf/subscribe [:elapsed-time])
        !player                    @(rf/subscribe [:player])
        player-ready?              (and @!player (> (.-readyState @!player) 0))]
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
         [audio-source]]
        [:div.flex
         [player/button [:i.fa-solid.fa-list] #(rf/dispatch [::events/toggle-media-queue])
          :show-on-mobile? true]
         [player/button
          [:i.fa-solid.fa-backward-step]
          #(when (and media-queue (not= media-queue-pos 0))
             (rf/dispatch [::events/change-media-queue-pos
                           (- media-queue-pos 1)]))
          :disabled? (not (and media-queue (not= media-queue-pos 0)))]
         [player/button [:i.fa-solid.fa-backward] #(set! (.-currentTime @!player) (- @!elapsed-time 5))]
         [player/button
          (if @!player
            (if show-audio-player-loading?
              [loading/loading-icon service-color "text-1xl"]
              (if (.-paused @!player)
                [:i.fa-solid.fa-play]
                [:i.fa-solid.fa-pause]))
            [:i.fa-solid.fa-play])
          #(if (.-paused @!player)
             (.play @!player)
             (.pause @!player))
          :show-on-mobile? true]
         [player/button [:i.fa-solid.fa-forward] #(set! (.-currentTime @!player) (+ @!elapsed-time 5))]
         [player/button [:i.fa-solid.fa-forward-step]
          #(when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
             (rf/dispatch [::events/change-media-queue-pos
                           (+ media-queue-pos 1)]))
          :disabled? (not (and media-queue (< (+ media-queue-pos 1) (count media-queue))))]
         [:div.hidden.lg:flex.items-center
          [:span.mx-2 (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
          [:div.w-20.lg:w-56.mx-2
           [player/time-slider !player !elapsed-time service-color]]
          [:span.mx-2
           (if player-ready? (util/format-duration (.-duration @!player)) "00:00")]
          [player/button
           [:div.relative
            [:i.fa-solid.fa-repeat
             {:style {:color (when loop-playback service-color)}}]
            (when (= loop-playback :stream)
              [:span.absolute.font-bold
               {:style {:color     (when loop-playback service-color)
                        :font-size "6px"
                        :right     "6px"
                        :top       "6.5px"}}
               "1"])]
           #(rf/dispatch [::events/toggle-loop-playback])]
          [:div.hidden.lg:flex.items-center
           [player/button
            (if (or (and @!player muted?)) [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
            #(rf/dispatch [::events/toggle-mute @!player])]
           [player/volume-slider !player volume-level service-color]]]
         [player/button [:i.fa-solid.fa-close]
          (fn []
            (rf/dispatch [::events/toggle-audio-player])
            (.pause @!player)
            (set! (.-currentTime @!player) 0))
          :show-on-mobile? true]]]])))
