(ns tubo.stream.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.comments.views :as comments]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defn metadata-popover
  [{:keys [service-id url] :as stream}]
  (let [favorited? @(rf/subscribe [:bookmarks/favorited url])]
    [layout/popover
     [{:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:bg-player/show stream true])}
      {:label    "Start radio"
       :icon     [:i.fa-solid.fa-tower-cell]
       :on-click #(rf/dispatch [:bg-player/start-radio stream])}
      {:label    (if favorited? "Remove favorite" "Favorite")
       :icon     (if favorited?
                   [:i.fa-solid.fa-heart
                    {:style {:color (utils/get-service-color service-id)}}]
                   [:i.fa-solid.fa-heart])
       :on-click #(rf/dispatch [(if favorited? :likes/remove :likes/add) stream
                                true])}
      {:label "Original"
       :link  {:route url :external? true}
       :icon  [:i.fa-solid.fa-external-link-alt]}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [modals/add-to-bookmark stream]])}]
     :tooltip-classes ["right-5" "top-5"]
     :extra-classes ["p-3" "xs:py-2" "xs:px-4"]]))

(defn metadata-uploader
  [{:keys [uploader-url uploader-name subscriber-count] :as stream}]
  [:div.flex.items-center
   [layout/uploader-avatar stream]
   [:div.mx-3.gap-x-2
    [:a.line-clamp-1.font-semibold
     {:href  (rfe/href :channel-page nil {:url uploader-url})
      :title uploader-name}
     uploader-name]
    (when subscriber-count
      [:div.flex.items-center.text-neutral-600.dark:text-neutral-400.text-sm
       [:span {:title subscriber-count}
        (str (utils/format-quantity subscriber-count) " subscribers")]])]])

(defn metadata-stats
  [{:keys [like-count dislike-count] :as stream}]
  [:div.flex.items-center.justify-end.gap-x-2
   (when (or like-count dislike-count)
     [:div.flex.bg-neutral-200.dark:bg-neutral-800.px-4.py-2.rounded-full.sm:text-base.text-sm.gap-x-4
      (when like-count
        [:div.flex.items-center.gap-x-2
         [:i.fa-solid.fa-thumbs-up]
         [:span (utils/format-quantity like-count)]])
      (when dislike-count
        [:div.flex.items-center.gap-x-2
         [:i.fa-solid.fa-thumbs-down]
         [:span dislike-count]])])
   [:div.hidden.xs:flex.bg-neutral-200.dark:bg-neutral-800.rounded-full
    [metadata-popover stream]]])

(defn metadata
  [{:keys [name view-count upload-date] :as stream}]
  [:<>
   [:div.flex.items-center.justify-between
    [:h1.text-lg.sm:text-2xl.font-bold.line-clamp-2 {:title name} name]]
   [:div.flex.gap-x-2.text-neutral-600.dark:text-neutral-400.text-xs.sm:text-sm.my-1
    [:span {:title view-count}
     (str (utils/format-quantity view-count) " views")]
    [:span
     {:dangerouslySetInnerHTML {:__html "&bull;"} :style {:font-size "0.5rem"}}]
    [:span {:title upload-date} (utils/format-date-ago upload-date)]]
   [:div.flex.justify-between.py-4.flex-nowrap
    [metadata-uploader stream]
    [metadata-stats stream]]])

(defn description
  [{:keys [description show-description tags]}]
  (let [show? (:show-description @(rf/subscribe [:settings]))]
    (when (and show? (seq description))
      [:div.bg-neutral-200.dark:bg-neutral-800.p-4.rounded-lg.break-words.flex.flex-col.gap-y-2
       [layout/show-more-container show-description description
        #(rf/dispatch [(if @(rf/subscribe [:main-player/show])
                         :main-player/toggle-layout
                         :stream/toggle-layout)
                       :show-description])]
       (when-not (empty? tags)
         [:div.flex.gap-2.py-2.flex-wrap
          (for [[i tag] (map-indexed vector tags)]
            ^{:key i}
            [:span.bg-neutral-300.dark:bg-neutral-700.rounded-lg.px-2.py-1.text-xs.text-neutral-700.dark:text-neutral-300.flex.gap-x-1.items-center
             [:i.fa-solid.fa-tag]
             [:span.line-clamp-1 tag]])])])))

(defn comments
  [{:keys [comments-page show-comments show-comments-loading] :as stream}]
  (let [show?         (:show-comments @(rf/subscribe [:settings]))
        service-color @(rf/subscribe [:service-color])]
    (when show?
      (if show-comments-loading
        [:div.h-44.flex.items-center
         [layout/loading-icon service-color "text-2xl"]]
        (when (and show-comments comments-page)
          [comments/comments comments-page stream])))))

(defn related-items
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{:keys [related-streams]}]
      (let [show? (:show-related @(rf/subscribe [:settings]))]
        (when (and show? (seq related-streams))
          [:div
           [:div.flex.flex-wrap.items-center.justify-between.mt-8.min-w-full
            [:div.flex.gap-x-4.items-center
             [:span.font-semibold.text-xl "Next Up"]
             [layout/popover
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [:queue/add-n
                                         related-streams
                                         true])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [:modals/open
                                         [modals/add-to-bookmark
                                          related-streams]])}]]]
            [items/layout-switcher !layout]
            [items/related-streams related-streams nil !layout]]])))))

(defn stream
  []
  (let [!active-tab (r/atom :comments)]
    (fn []
      (let [stream        @(rf/subscribe [:stream])
            !player       @(rf/subscribe [:main-player])
            page-loading? @(rf/subscribe [:show-page-loading])]
        [:<>
         (when-not page-loading?
           [:div.flex.flex-col.justify-center.items-center
            [player/video-player stream !player {}
             #(rf/dispatch [:player/set-stream stream !player])]])
         [layout/content-container
          [metadata stream]
          [description stream]
          [:div.mt-5
           [layout/tabs
            [{:id        :comments
              :label     "Comments"
              :left-icon [:i.fa-solid.fa-comments]}
             {:id        :related-items
              :label     "Related Items"
              :left-icon [:i.fa-solid.fa-images]}]
            :selected-id @!active-tab
            :on-change #(reset! !active-tab %)]]
          (case @!active-tab
            :comments      [comments stream]
            :related-items [related-items stream]
            [comments stream])]]))))
