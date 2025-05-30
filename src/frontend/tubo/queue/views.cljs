(ns tubo.queue.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bg-player.views :as bg-player]
   [tubo.bookmarks.modals :as modals]
   [tubo.player.views :as player]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn item-metadata
  [{:keys [uploader-name name service-id] :as item} queue-pos i]
  [:div.flex.cursor-pointer.py-2
   {:class    (when (= i queue-pos) ["bg-neutral-200" "dark:bg-neutral-900"])
    :on-click #(rf/dispatch [:queue/change-pos i])}
   [:div.flex.items-center.justify-center.min-w-16.w-16.xs:min-w-24.xs:w-24
    [:span.font-bold.text-neutral-400.text-sm
     (if (= i queue-pos) [:i.fa-solid.fa-play] (inc i))]]
   [:div.flex.items-center.shrink-0.grow-0
    [layout/thumbnail item nil :classes
     ["h-12" "xs:h-16" "w-16" "xs:w-24" "md:w-32"]]]
   [:div.flex.flex-col.pl-4.pr-12.w-full
    [:h1.line-clamp-1.w-fit.text-sm.xs:text-lg {:title name} name]
    [:div.text-neutral-600.dark:text-neutral-400.text-xs.xs:text-sm.flex.flex-col.xs:flex-row
     [:span.line-clamp-1 {:title uploader-name} uploader-name]
     (when service-id
       [:<>
        [:span.px-2.hidden.xs:inline-block
         {:dangerouslySetInnerHTML {:__html "&bull;"}
          :style                   {:font-size "0.5rem"}}]
        [:span (utils/get-service-name service-id)]])]]])

(defn popover
  [{:keys [uploader-url] :as item} i]
  [:div.absolute.right-0.top-0.min-h-full.flex.items-center
   [layout/popover
    [{:label    "Start radio"
      :icon     [:i.fa-solid.fa-tower-cell]
      :on-click #(rf/dispatch [:bg-player/start-radio item])}
     {:label    "Add to playlist"
      :icon     [:i.fa-solid.fa-plus]
      :on-click #(rf/dispatch [:modals/open [modals/add-to-bookmark item]])}
     {:label    "Remove from queue"
      :icon     [:i.fa-solid.fa-trash]
      :on-click #(rf/dispatch [:queue/remove i])}
     {:label    "Show channel details"
      :icon     [:i.fa-solid.fa-user]
      :on-click #(rf/dispatch [:navigation/navigate
                               {:name   :channel-page
                                :params {}
                                :query  {:url uploader-url}}])}]
    :tooltip-classes ["right-5" "top-0" "z-20"]
    :extra-classes [:px-7 :py-2]]])

(defn queue-item
  [item queue queue-pos i bookmarks]
  [:div.relative.w-full
   {:ref #(when (and queue (= queue-pos i)) (rf/dispatch [:scroll-top %]))}
   [item-metadata item queue-pos i]
   [popover item i bookmarks]])

(defn queue-metadata
  [{:keys [url name uploader-url uploader-name]}]
  [:div.flex.flex-col.py-2
   [:a.text-md.line-clamp-1.w-fit
    {:href  (rfe/href :stream-page nil {:url url})
     :title name}
    name]
   [:a.text-sm.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit
    {:href  (rfe/href :channel-page nil {:url uploader-url})
     :title uploader-name}
    uploader-name]])

(defn main-controls
  [color]
  (let [loop-playback    @(rf/subscribe [:player/loop])
        shuffle?         @(rf/subscribe [:player/shuffled])
        !player          @(rf/subscribe [:bg-player])
        loading?         @(rf/subscribe [:bg-player/loading])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        paused?          @(rf/subscribe [:player/paused])
        !elapsed-time    @(rf/subscribe [:elapsed-time])
        queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])]
    [:<>
     [:div.flex.flex-auto.py-2.w-full.items-center.text-sm
      [:span.mr-4.whitespace-nowrap
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [bg-player/time-slider !player !elapsed-time color]
      [:span.ml-4.whitespace-nowrap
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]
     [:div.flex.justify-center.items-center.gap-x-4
      [player/loop-button loop-playback color true]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon
       (if (and (not loading?)
                (not waiting?)
                (or (nil? bg-player-ready?) @!player))
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon color :text-3xl])
       :on-click
       #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes [:text-3xl]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/shuffle-button shuffle? color true]]]))

(defn queue
  []
  (let [show-queue @(rf/subscribe [:queue/show])
        stream     @(rf/subscribe [:queue/current])
        bookmarks  @(rf/subscribe [:bookmarks])
        queue-pos  @(rf/subscribe [:queue/position])
        queue      @(rf/subscribe [:queue])
        color      (-> stream
                       :service-id
                       utils/get-service-color)]
    [:div.fixed.flex.flex-col.items-center.justify-center.backdrop-blur.z-10.w-full.left-0
     {:class ["dark:bg-neutral-950/90" "bg-neutral-100/90"
              "min-h-[calc(100dvh-56px)]" "h-[calc(100dvh-56px)]"
              (when-not show-queue "invisible")
              (if show-queue "opacity-1" "opacity-0")]}
     [:div.flex.flex-col.flex-auto.h-full.lg:pt-5.w-full
      {:class ["lg:w-4/5" "xl:w-3/5"]}
      [:div.flex.flex-col.overflow-y-auto.scrollbar-none.flex-auto.gap-y-1.relative.scroll-smooth
       (for [[i item] (map-indexed vector queue)]
         ^{:key i}
         [queue-item item queue queue-pos i bookmarks])]
      [:div.flex.flex-col.py-4.shrink-0.px-5
       [queue-metadata stream]
       [main-controls color]]]]))
