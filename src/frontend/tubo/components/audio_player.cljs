(ns tubo.components.audio-player
  (:require
   [goog.object :as gobj]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.layout :as layout]
   [tubo.components.modals.bookmarks :as bookmarks]
   [tubo.components.player :as player]
   [tubo.events :as events]
   [tubo.utils :as utils]))

(defn audio-source
  [!player]
  (let [{:keys [stream]} @(rf/subscribe [:media-queue-stream])
        media-queue-pos  @(rf/subscribe [:media-queue-pos])]
    (r/create-class
     {:display-name "AudioPlayer"
      :component-did-mount
      (fn [this]
        (set! (.-onended (rdom/dom-node this))
              #(rf/dispatch [::events/change-media-queue-pos (+ media-queue-pos 1)]))
        (when stream
          (set! (.-src (rdom/dom-node this)) stream)))
      :reagent-render
      (fn [!player]
        (let [!elapsed-time @(rf/subscribe [:elapsed-time])
              muted?        @(rf/subscribe [:muted])
              volume-level  @(rf/subscribe [:volume-level])
              loop-playback @(rf/subscribe [:loop-playback])]
          [:audio
           {:ref            #(reset! !player %)
            :loop           (= loop-playback :stream)
            :muted          muted?
            :on-loaded-data #(rf/dispatch [::events/player-start])
            :on-time-update #(reset! !elapsed-time (.-currentTime @!player))
            :on-pause       #(rf/dispatch [::events/change-player-paused true])
            :on-play        #(rf/dispatch [::events/change-player-paused false])}]))})))

(defn main-controls
  [service-color]
  (let [media-queue     @(rf/subscribe [:media-queue])
        media-queue-pos @(rf/subscribe [:media-queue-pos])
        loading?        @(rf/subscribe [:show-audio-player-loading])
        !elapsed-time   @(rf/subscribe [:elapsed-time])
        !player         @(rf/subscribe [:player])
        paused?         @(rf/subscribe [:paused])
        player-ready?   @(rf/subscribe [:player-ready])
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
       (if (or (not loading?) player-ready?)
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon service-color "lg:text-2xl"])
       #(rf/dispatch [::events/set-player-paused (not paused?)])
       :show-on-mobile? true
       :extra-classes "lg:text-2xl"]
      [player/button [:i.fa-solid.fa-forward]
       #(rf/dispatch [::events/set-player-time (+ @!elapsed-time 5)])]
      [player/button [:i.fa-solid.fa-forward-step]
       #(when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
          (rf/dispatch [::events/change-media-queue-pos (+ media-queue-pos 1)]))
       :disabled? (not (and media-queue (< (+ media-queue-pos 1) (count media-queue))))]]
     [:div.hidden.lg:flex.items-center.text-sm
      [:span.mx-2
       (if (and @!player @!elapsed-time) (utils/format-duration @!elapsed-time) "00:00")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       [player/time-slider !player !elapsed-time service-color]]
      [:span.mx-2
       (if (and @!player player-ready?) (utils/format-duration (.-duration @!player)) "00:00")]]]))

(defn player
  []
  (let [!menu-active? (r/atom nil)]
    (fn []
      (let [{:keys
             [uploader-name uploader-url thumbnail-url
              name stream url service-id] :as current-stream}
            @(rf/subscribe [:media-queue-stream])
            show-audio-player? @(rf/subscribe [:show-audio-player])
            show-media-queue?  @(rf/subscribe [:show-media-queue])
            volume-level       @(rf/subscribe [:volume-level])
            muted?             @(rf/subscribe [:muted])
            bookmarks          @(rf/subscribe [:bookmarks])
            !player            @(rf/subscribe [:player])
            media-queue-pos    @(rf/subscribe [:media-queue-pos])
            {:keys [theme]}    @(rf/subscribe [:settings])
            service-color      (and service-id (utils/get-service-color service-id))
            bg-color           (str "rgba(" (if (= theme "dark") "23, 23, 23" "255, 255, 255") ", 0.95)")
            liked?             (some #(= (:url %) url) (-> bookmarks first :items))]
        (when show-audio-player?
          [:div.sticky.bottom-0.z-10.p-3.absolute.box-border.m-0.transition-all.ease-in.delay-0
           {:style
            {:visibility         (when show-media-queue? "hidden")
             :opacity            (if show-media-queue? 0 1)
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
               {:href (rfe/href :tubo.routes/stream nil {:url url})
                :title name}
               name]
              [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})
                :title uploader-name}
               uploader-name]]
             [audio-source !player]]
            [main-controls service-color]
            [:div.flex.lg:justify-end.lg:flex-1
             [player/volume-slider !player volume-level muted? service-color]
             [player/button [:i.fa-solid.fa-list] #(rf/dispatch [::events/show-media-queue true])
              :show-on-mobile? true
              :extra-classes "pl-4 pr-3"]
             [layout/popover-menu !menu-active?
              [{:label    (if liked? "Remove favorite" "Favorite")
                :icon     [:i.fa-solid.fa-heart (when liked? {:style {:color service-color}})]
                :on-click #(rf/dispatch [(if liked? ::events/remove-from-likes ::events/add-to-likes) current-stream])}
               {:label    "Play radio"
                :icon     [:i.fa-solid.fa-tower-cell]
                :on-click #(rf/dispatch [::events/start-stream-radio current-stream])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [::events/add-bookmark-list-modal
                                         [bookmarks/add-to-bookmark-list-modal current-stream]])}
               {:label    "Remove from queue"
                :icon     [:i.fa-solid.fa-trash]
                :on-click #(rf/dispatch [::events/remove-from-media-queue media-queue-pos])}
               {:label    "Close player"
                :icon     [:i.fa-solid.fa-close]
                :on-click #(rf/dispatch [::events/dispose-audio-player])}]
              :menu-styles {:bottom "30px" :top nil :right "30px"}
              :extra-classes "pt-1 !pl-3 pr-3"]]]])))))
