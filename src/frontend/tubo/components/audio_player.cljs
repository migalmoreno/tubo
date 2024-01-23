(ns tubo.components.audio-player
  (:require
   [goog.object :as gobj]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.layout :as layout]
   [tubo.components.player :as player]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn audio-source
  [!player]
  (let [{:keys [stream]} @(rf/subscribe [:media-queue-stream])]
    (r/create-class
     {:display-name "AudioPlayer"
      :component-did-mount
      (fn [this]
        (when stream
          (set! (.-src (rdom/dom-node this)) stream)))
      :reagent-render
      (fn [!player]
        (let [!elapsed-time @(rf/subscribe [:elapsed-time])
              player-ready? (and @!player (> (.-readyState @!player) 0))
              muted?        @(rf/subscribe [:muted])
              volume-level  @(rf/subscribe [:volume-level])
              loop-playback @(rf/subscribe [:loop-playback])]
          [:audio
           {:ref            #(reset! !player %)
            :loop           (= loop-playback :stream)
            :on-loaded-data #(rf/dispatch [::events/player-start])
            :muted          muted?
            :on-time-update #(reset! !elapsed-time (.-currentTime @!player))}]))})))

(defn main-controls
  [service-color]
  (let [media-queue     @(rf/subscribe [:media-queue])
        media-queue-pos @(rf/subscribe [:media-queue-pos])
        loading?        @(rf/subscribe [:show-audio-player-loading])
        !elapsed-time   @(rf/subscribe [:elapsed-time])
        !player         @(rf/subscribe [:player])
        paused?         @(rf/subscribe [:paused])
        player-ready?   (and @!player (> (.-readyState @!player) 0))
        loop-playback   @(rf/subscribe [:loop-playback])]
    [:div.flex.flex-col.items-center.ml-auto
     [:div.flex.justify-end
      [player/loop-button loop-playback service-color]
      [player/button
       [:i.fa-solid.fa-backward-step]
       #(when (and media-queue (not= media-queue-pos 0))
          (rf/dispatch [::events/change-media-queue-pos (- media-queue-pos 1)]))
       :disabled? (not (and media-queue (not= media-queue-pos 0)))]
      [player/button [:i.fa-solid.fa-backward]
       #(rf/dispatch [::events/set-player-time (- @!elapsed-time 5)])]
      [player/button
       (if (or loading? (not @!player))
         [layout/loading-icon service-color "lg:text-2xl"]
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause]))
       #(rf/dispatch [::events/player-paused (not paused?)])
       :show-on-mobile? true
       :extra-styles "lg:text-2xl"]
      [player/button [:i.fa-solid.fa-forward]
       #(rf/dispatch [::events/set-player-time (+ @!elapsed-time 5)])]
      [player/button [:i.fa-solid.fa-forward-step]
       #(when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
          (rf/dispatch [::events/change-media-queue-pos (+ media-queue-pos 1)]))
       :disabled? (not (and media-queue (< (+ media-queue-pos 1) (count media-queue))))]]
     [:div.hidden.lg:flex.items-center
      [:span.mx-2.text-sm
       (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       [player/time-slider !player !elapsed-time service-color]]
      [:span.mx-2.text-sm
       (if player-ready? (util/format-duration (.-duration @!player)) "00:00")]]]))

(defn player
  []
  (let [{:keys
         [uploader-name uploader-url thumbnail-url
          name stream url service-color] :as current-stream}
        @(rf/subscribe [:media-queue-stream])
        show-audio-player?         @(rf/subscribe [:show-audio-player])
        show-media-queue?          @(rf/subscribe [:show-media-queue])
        volume-level               @(rf/subscribe [:volume-level])
        muted?                     @(rf/subscribe [:muted])
        !player                    @(rf/subscribe [:player])
        {:keys [current-theme]}    @(rf/subscribe [:settings])
        bg-color (str "rgba(" (if (= current-theme "dark") "23, 23, 23" "255, 255, 255") ", 0.95)")]
    (when show-audio-player?
      [:div.sticky.bottom-0.z-40.p-3.absolute.box-border.m-0
       {:style
        {:display            (when show-media-queue? "none")
         :background-image   (str "linear-gradient(0deg, " bg-color "," bg-color "), url(\"" thumbnail-url "\")")
         :backgroundSize     "cover"
         :backgroundPosition "center"
         :backgroundRepeat   "no-repeat"}}
       [:div.flex.items-center.justify-between
        [:div.flex.items-center.lg:flex-1
         [:div {:style {:height "40px" :width "70px" :maxWidth "70px" :minWidth "70px"}}
          [:img.min-h-full.max-h-full.object-cover.min-w-full.max-w-full.w-full {:src thumbnail-url}]]
         [:div.flex.flex-col.px-2
          [:a.text-xs.line-clamp-1
           {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
          [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
           {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
         [audio-source !player]]
        [main-controls service-color]
        [:div.flex.lg:justify-end.lg:flex-1
         [player/volume-slider !player volume-level muted? service-color]
         [player/button [:i.fa-solid.fa-list] #(rf/dispatch [::events/toggle-media-queue])
          :show-on-mobile? true]
         [player/button [:i.fa-solid.fa-close] #(rf/dispatch [::events/dispose-audio-player])
          :show-on-mobile? true]]]])))
