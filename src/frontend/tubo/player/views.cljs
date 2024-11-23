(ns tubo.player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.components.layout :as layout]
   [tubo.components.player :as player]
   [tubo.queue.views :as queue]
   [tubo.stream.views :as stream]
   [tubo.utils :as utils]))

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
  (let [queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue-pos])
        loading?         @(rf/subscribe [:bg-player/loading])
        loop-playback    @(rf/subscribe [:loop-playback])
        shuffle?         @(rf/subscribe [:shuffle])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        paused?          @(rf/subscribe [:paused])
        !elapsed-time    @(rf/subscribe [:elapsed-time])]
    [:div.flex.flex-col.items-center.ml-auto
     [:div.flex.justify-end
      [player/loop-button loop-playback color]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])]
      [player/button
       :icon
       (if (and (not loading?) @!player)
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon color "lg:text-2xl"])
       :on-click
       #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes ["lg:text-2xl"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]
      [player/shuffle-button shuffle? color]]
     [:div.hidden.lg:flex.items-center.text-sm
      [:span.mx-2
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       [player/time-slider !player !elapsed-time color]]
      [:span.mx-2
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]]))

(defn extra-controls
  [_!player _stream _color]
  (let [!menu-active? (r/atom nil)]
    (fn [!player {:keys [url uploader-url] :as stream} color]
      (let [muted?    @(rf/subscribe [:muted])
            volume    @(rf/subscribe [:volume-level])
            queue     @(rf/subscribe [:queue])
            queue-pos @(rf/subscribe [:queue-pos])
            bookmarks @(rf/subscribe [:bookmarks])
            liked?    (some #(= (:url %) url)
                            (-> bookmarks
                                first
                                :items))
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
            :icon     [:i.fa-solid.fa-heart
                       (when liked? {:style {:color color}})]
            :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add)
                                     stream])}
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
           {:label    "Switch to main"
            :icon     [:i.fa-solid.fa-display]
            :on-click #(rf/dispatch [:player/switch-to-main])}
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}
           {:label    "Close player"
            :icon     [:i.fa-solid.fa-close]
            :on-click #(rf/dispatch [:bg-player/dispose])}]
          :menu-styles {:bottom "30px" :top nil :right "10px"}
          :extra-classes [:pt-1 :!pl-4 :px-3]]]))))

(defn background-player
  []
  (let [!player      @(rf/subscribe [:player])
        stream       @(rf/subscribe [:queue-stream])
        show-queue?  @(rf/subscribe [:show-queue])
        show-player? @(rf/subscribe [:bg-player/show])
        dark-theme?  @(rf/subscribe [:dark-theme])
        color        (-> stream
                         :service-id
                         utils/get-service-color)
        bg-color     (str "rgba("
                          (if dark-theme? "23,23,23" "255,255,255")
                          ",0.95)")
        bg-image     (str "linear-gradient("
                          bg-color
                          ","
                          bg-color
                          "),url("
                          (:thumbnail-url stream)
                          ")")]
    [:div.sticky.absolute.left-0.bottom-0.z-10.p-3.transition-all.ease-in.relative
     {:style
      {:visibility          (when (or (not show-player?) show-queue?) "hidden")
       :opacity             (if (or (not show-player?) show-queue?) 0 1)
       :background-image    bg-image
       :background-size     "cover"
       :background-position "center"
       :background-repeat   "no-repeat"}}
     [:div.flex.items-center
      [player/audio-player stream !player]
      [stream-metadata stream]
      [main-controls !player color]
      [extra-controls !player stream color]]]))

(defn main-player
  []
  (let [queue        @(rf/subscribe [:queue])
        queue-pos    @(rf/subscribe [:queue-pos])
        bookmarks    @(rf/subscribe [:bookmarks])
        !player      @(rf/subscribe [:main-player])
        stream       @(rf/subscribe [:queue-stream])
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
         [stream/suggested stream]]])]))
