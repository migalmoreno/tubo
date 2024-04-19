(ns tubo.components.play-queue
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.modals.bookmarks :as bookmarks]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.player :as player]
   [tubo.events :as events]
   [tubo.utils :as utils]))

(defn play-queue-item
  [item media-queue-pos i bookmarks]
  (let [!menu-active? (r/atom false)]
    (fn [{:keys [service-id uploader-name uploader-url name duration
                 stream url thumbnail-url] :as item}
         media-queue-pos i bookmarks]
      (let [liked?          (some #(= (:url %) url) (-> bookmarks first :items))
            media-queue-pos @(rf/subscribe [:media-queue-pos])
            media-queue     @(rf/subscribe [:media-queue])]
        [:div.relative.w-full
         {:ref #(when (= media-queue-pos i)
                  (rf/dispatch [::events/scroll-into-view %]))}
         [:div.flex.cursor-pointer.py-2
          {:class    (when (= i media-queue-pos) "bg-[#f0f0f0] dark:bg-stone-800")
           :on-click #(rf/dispatch [::events/change-media-queue-pos i])}
          [:div.flex.items-center.justify-center.min-w-20.w-20.xs:min-w-28.xs:w-28
           [:span.font-bold.text-neutral-400.text-sm
            (if (= i media-queue-pos) [:i.fa-solid.fa-play] (inc i))]]
          [:div.w-36
           [layout/thumbnail thumbnail-url nil name duration :classes "h-16 !p-0" :rounded? false]]
          [:div.flex.flex-col.pl-4.pr-12.w-full
           [:h1.line-clamp-1 {:title name} name]
           [:div.text-neutral-600.dark:text-neutral-300.text-sm.flex.flex-col.xs:flex-row
            [:span.line-clamp-1 {:title uploader-name} uploader-name]
            [:span.px-2.hidden.xs:inline-block {:dangerouslySetInnerHTML {:__html "&bull;"}}]
            [:span (utils/get-service-name service-id)]]]]
         [:div.absolute.right-0.top-0.min-h-full.flex.items-center
          [layout/popover-menu !menu-active?
           [{:label    (if liked? "Remove favorite" "Favorite")
             :icon     [:i.fa-solid.fa-heart (when liked? {:style {:color (utils/get-service-color service-id)}})]
             :on-click #(rf/dispatch [(if liked? ::events/remove-from-likes ::events/add-to-likes) item])}
            {:label    "Play radio"
             :icon     [:i.fa-solid.fa-tower-cell]
             :on-click #(rf/dispatch [::events/start-stream-radio item])}
            {:label    "Add to playlist"
             :icon     [:i.fa-solid.fa-plus]
             :on-click #(rf/dispatch [::events/add-bookmark-list-modal
                                      [bookmarks/add-to-bookmark-list-modal item]])}
            {:label    "Remove from queue"
             :icon     [:i.fa-solid.fa-trash]
             :on-click #(rf/dispatch [::events/remove-from-media-queue i])}]
           :menu-styles {:right "40px"}
           :extra-classes "px-7 py-2"]]]))))

(defn queue
  []
  (let [show-media-queue       @(rf/subscribe [:show-media-queue])
        loading?               @(rf/subscribe [:show-audio-player-loading])
        paused?                @(rf/subscribe [:paused])
        media-queue            @(rf/subscribe [:media-queue])
        media-queue-pos        @(rf/subscribe [:media-queue-pos])
        {:keys [uploader-name uploader-url name stream url service-id]
         :as   current-stream} @(rf/subscribe [:media-queue-stream])
        service-color          (and service-id (utils/get-service-color service-id))
        !elapsed-time          @(rf/subscribe [:elapsed-time])
        !player                @(rf/subscribe [:player])
        loop-playback          @(rf/subscribe [:loop-playback])
        player-ready?          @(rf/subscribe [:player-ready])
        bookmarks              @(rf/subscribe [:bookmarks])]
    [:div.fixed.flex.flex-col.items-center.min-w-full.w-full.z-10.backdrop-blur
     {:style {:minHeight  "calc(100dvh - 56px)"
              :height     "calc(100dvh - 56px)"
              :visibility (when-not show-media-queue "hidden")
              :opacity    (if show-media-queue 1 0)}
      :class "dark:bg-neutral-900/90 bg-white/90 backdrop-blur"}
     [layout/focus-overlay #(rf/dispatch [::events/show-media-queue false]) show-media-queue true]
     [:div.z-20.w-full.flex.flex-col.flex-auto.h-full.lg:pt-5
      {:class "lg:w-4/5 xl:w-3/5"}
      [:div.flex.flex-col.overflow-y-auto.flex-auto.gap-y-1
       (for [[i item] (map-indexed vector media-queue)]
         ^{:key i} [play-queue-item item media-queue-pos i bookmarks])]
      [:div.flex.flex-col.py-4.shrink-0.px-5
       [:div.flex.flex-col.w-full.py-2
        [:a.text-md.line-clamp-1
         {:href  (rfe/href :tubo.routes/stream nil {:url url})
          :title name}
         name]
        [:a.text-sm.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
         {:href  (rfe/href :tubo.routes/channel nil {:url uploader-url})
          :title uploader-name}
         uploader-name]]
       [:div.flex.flex-auto.py-2.w-full.items-center.text-sm
        [:span.mr-4 (if (and @!elapsed-time @!player) (utils/format-duration @!elapsed-time) "00:00")]
        [player/time-slider !player !elapsed-time service-color]
        [:span.ml-4 (if (and player-ready? @!player) (utils/format-duration (.-duration @!player)) "00:00")]]
       [:div.flex.justify-center.items-center
        [player/loop-button loop-playback service-color true]
        [player/button
         [:i.fa-solid.fa-backward-step]
         #(when (and media-queue (not= media-queue-pos 0))
            (rf/dispatch [::events/change-media-queue-pos
                          (- media-queue-pos 1)]))
         :disabled? (not (and media-queue (not= media-queue-pos 0)))
         :extra-classes "text-xl"
         :show-on-mobile? true]
        [player/button
         [:i.fa-solid.fa-backward]
         #(set! (.-currentTime @!player) (- @!elapsed-time 5))
         :extra-classes "text-xl"
         :show-on-mobile? true]
        [player/button
         (if (or loading? (not player-ready?))
           [layout/loading-icon service-color "text-3xl"]
           (if paused?
             [:i.fa-solid.fa-play]
             [:i.fa-solid.fa-pause]))
         #(rf/dispatch [::events/set-player-paused (not paused?)])
         :extra-classes "text-3xl"
         :show-on-mobile? true]
        [player/button
         [:i.fa-solid.fa-forward]
         #(set! (.-currentTime @!player) (+ @!elapsed-time 5))
         :extra-classes "text-xl"
         :show-on-mobile? true]
        [player/button
         [:i.fa-solid.fa-forward-step]
         #(when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
            (rf/dispatch [::events/change-media-queue-pos
                          (+ media-queue-pos 1)]))
         :disabled? (not (and media-queue (< (+ media-queue-pos 1) (count media-queue))))
         :extra-classes "text-xl"
         :show-on-mobile? true]
        [player/button [:i.fa-solid.fa-list] #(rf/dispatch [::events/show-media-queue false])
         :show-on-mobile? true
         :extra-classes "pl-4 pr-3"]]]]]))
