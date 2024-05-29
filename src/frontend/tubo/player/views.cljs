(ns tubo.player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.components.layout :as layout]
   [tubo.components.player :as player]
   [tubo.utils :as utils]
   ["video.js" :as videojs]
   ["videojs-mobile-ui"]
   ["@silvermine/videojs-quality-selector" :as VideojsQualitySelector]))

(defn audio
  [!player]
  (let [{:keys [stream]} @(rf/subscribe [:queue-stream])
        queue-pos        @(rf/subscribe [:queue-pos])]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (set! (.-onended (rdom/dom-node this))
              #(rf/dispatch [:queue/change-pos (inc queue-pos)]))
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
            :on-loaded-data #(rf/dispatch [:player/start-in-background])
            :on-time-update #(reset! !elapsed-time (.-currentTime @!player))
            :on-pause       #(rf/dispatch [:player/set-paused true])
            :on-play        #(rf/dispatch [:player/set-paused false])}]))})))

(defn stream-metadata
  [{:keys [thumbnail-url url name uploader-url uploader-name]}]
  [:div.flex.items-center.lg:flex-1
   [:div
    [layout/thumbnail thumbnail-url (rfe/href :stream-page nil {:url url})
     name nil :classes [:h-14 :py-2 "w-[70px]"]]]
   [:div.flex.flex-col.px-2
    [:a.text-xs.line-clamp-1
     {:href  (rfe/href :stream-page nil {:url url})
      :title name}
     name]
    [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
     {:href  (rfe/href :channel-page nil {:url uploader-url})
      :title uploader-name}
     uploader-name]]])

(defn main-controls
  [!player color]
  (let [queue          @(rf/subscribe [:queue])
        queue-pos      @(rf/subscribe [:queue-pos])
        loading?       @(rf/subscribe [:show-background-player-loading])
        !elapsed-time  @(rf/subscribe [:elapsed-time])
        loop-playback  @(rf/subscribe [:loop-playback])
        paused?        @(rf/subscribe [:paused])
        player-ready?  @(rf/subscribe [:player-ready])]
    [:div.flex.flex-col.items-center.ml-auto
     [:div.flex.justify-end
      [player/loop-button loop-playback color]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:player/seek (- @!elapsed-time 5)])]
      [player/button
       :icon (if (or (not loading?) player-ready?)
               (if paused?
                 [:i.fa-solid.fa-play]
                 [:i.fa-solid.fa-pause])
               [layout/loading-icon color "lg:text-2xl"])
       :on-click #(rf/dispatch [:player/pause (not paused?)])
       :show-on-mobile? true
       :extra-classes ["lg:text-2xl"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:player/seek (+ @!elapsed-time 5)])]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]]
     [:div.hidden.lg:flex.items-center.text-sm
      [:span.mx-2
       (if @!elapsed-time (utils/format-duration @!elapsed-time) "--:--")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       [player/time-slider !player !elapsed-time color]]
      [:span.mx-2
       (if player-ready? (utils/format-duration (.-duration @!player)) "--:--")]]]))

(defn extra-controls
  [!player {:keys [url uploader-url] :as stream} color]
  (let [!menu-active? (r/atom nil)]
    (fn []
      (let [muted?    @(rf/subscribe [:muted])
            volume    @(rf/subscribe [:volume-level])
            queue     @(rf/subscribe [:queue])
            queue-pos @(rf/subscribe [:queue-pos])
            bookmarks @(rf/subscribe [:bookmarks])
            liked?    (some #(= (:url %) url) (-> bookmarks first :items))
            bookmark  #(rf/dispatch [:modals/open [modals/add-to-bookmark %]])]
        [:div.flex.lg:justify-end.lg:flex-1
         [player/volume-slider !player volume muted? color]
         [player/button
          :icon [:i.fa-solid.fa-list]
          :on-click #(rf/dispatch [:queue/show true])
          :show-on-mobile? true
          :extra-classes [:!pl-4 :!pr-3]]
         [layout/popover-menu !menu-active?
          [{:label    (if liked? "Remove favorite" "Favorite")
            :icon     [:i.fa-solid.fa-heart (when liked? {:style {:color color}})]
            :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add) stream])}
           {:label    "Play radio"
            :icon     [:i.fa-solid.fa-tower-cell]
            :on-click #(rf/dispatch [:player/start-radio stream])}
           {:label    "Add current to playlist"
            :icon     [:i.fa-solid.fa-plus]
            :on-click #(bookmark stream)}
           {:label    "Add queue to playlist"
            :icon     [:i.fa-solid.fa-list]
            :on-click #(bookmark queue)}
           {:label    "Remove from queue"
            :icon     [:i.fa-solid.fa-trash]
            :on-click #(rf/dispatch [:queue/remove queue-pos])}
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}
           {:label    "Close player"
            :icon     [:i.fa-solid.fa-close]
            :on-click #(rf/dispatch [:player/dispose])}]
          :menu-styles {:bottom "30px" :top nil :right "10px"}
          :extra-classes [:pt-1 :!pl-4 :px-3]]]))))

(defn background-player
  []
  (let [!player      @(rf/subscribe [:player])
        stream       @(rf/subscribe [:queue-stream])
        show-player? @(rf/subscribe [:show-background-player])
        show-queue?  @(rf/subscribe [:show-queue])
        dark-theme?  @(rf/subscribe [:dark-theme])
        color        (-> stream :service-id utils/get-service-color)
        bg-color     (str "rgba(" (if dark-theme? "23,23,23" "255,255,255") ",0.95)")
        bg-image     (str "linear-gradient(" bg-color "," bg-color "),url(" (:thumbnail-url stream) ")")]
    (when show-player?
      [:div.sticky.absolute.left-0.bottom-0.z-10.p-3.transition-all.ease-in
       {:style
        {:visibility          (when show-queue? "hidden")
         :opacity             (if show-queue? 0 1)
         :background-image    bg-image
         :background-size     "cover"
         :background-position "center"
         :background-repeat   "no-repeat"}}
       [:div.flex.items-center.justify-between
        [audio !player]
        [stream-metadata stream]
        [main-controls !player color]
        [extra-controls !player stream color]]])))

(defn main-player
  [options service-id]
  (let [!player (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [^videojs/VideoJsPlayer this]
        (VideojsQualitySelector videojs)
        (reset! !player (videojs (rdom/dom-node this) (clj->js options)))
        (.on @!player "ready" #(.mobileUi ^videojs/VideoJsPlayer @!player))
        (.on @!player "play" #(rf/dispatch [:player/start-in-main !player options service-id])))
      :component-will-unmount #(when @!player (.dispose @!player))
      :reagent-render         (fn [options] [:video-js.vjs-tubo])})))
