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
  [_]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [service-id url] :as stream}]
      (let [bookmarks @(rf/subscribe [:bookmarks])
            liked?    (some #(= (:url %) url)
                            (-> bookmarks
                                first
                                :items))]
        [layout/popover-menu !menu-active?
         [{:label    "Add to queue"
           :icon     [:i.fa-solid.fa-headphones]
           :on-click #(rf/dispatch [:bg-player/show stream true])}
          {:label    "Play radio"
           :icon     [:i.fa-solid.fa-tower-cell]
           :on-click #(rf/dispatch [:bg-player/start-radio stream])}
          {:label    (if liked? "Remove favorite" "Favorite")
           :icon     (if liked?
                       [:i.fa-solid.fa-heart
                        {:style {:color (utils/get-service-color service-id)}}]
                       [:i.fa-solid.fa-heart])
           :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add) stream
                                    true])}
          {:label "Original"
           :link  {:route url :external? true}
           :icon  [:i.fa-solid.fa-external-link-alt]}
          {:label    "Add to playlist"
           :icon     [:i.fa-solid.fa-plus]
           :on-click #(rf/dispatch [:modals/open
                                    [modals/add-to-bookmark stream]])}]]))))

(defn metadata-uploader
  [{:keys [uploader-url uploader-name subscriber-count] :as stream}]
  [:div.flex.items-center
   [layout/uploader-avatar stream]
   [:div.mx-3
    [:a.line-clamp-1.font-semibold
     {:href  (rfe/href :channel-page nil {:url uploader-url})
      :title uploader-name}
     uploader-name]
    (when subscriber-count
      [:div.flex.my-2.items-center
       [:i.fa-solid.fa-users.text-xs]
       [:p.mx-2 (utils/format-quantity subscriber-count)]])]])

(defn metadata-stats
  [{:keys [view-count like-count dislike-count upload-date]}]
  [:div.flex.flex-col.items-end.flex-auto.justify-center
   (when view-count
     [:div.sm:text-base.text-sm.mb-1
      [:i.fa-solid.fa-eye]
      [:span.ml-2 (.toLocaleString view-count)]])
   [:div.flex
    (when like-count
      [:div.items-center.sm:text-base.text-sm
       [:i.fa-solid.fa-thumbs-up]
       [:span.ml-2 (.toLocaleString like-count)]])
    (when dislike-count
      [:div.ml-2.items-center.sm:text-base.text-sm
       [:i.fa-solid.fa-thumbs-down]
       [:span.ml-2 dislike-count]])]
   (when upload-date
     [:div.sm:text-base.text-sm.mt-1.whitespace-nowrap
      [:i.fa-solid.fa-calendar]
      [:span.ml-2 (utils/format-date-string upload-date)]])])

(defn metadata
  [{:keys [name] :as stream}]
  [:<>
   [:div.flex.items-center.justify-between.mt-3
    [:h1.text-lg.sm:text-2xl.font-bold.line-clamp-1 {:title name} name]
    [:div.hidden.lg:block
     [metadata-popover stream]]]
   [:div.flex.justify-between.py-6.flex-nowrap
    [metadata-uploader stream]
    [metadata-stats stream]]])

(defn description
  [{:keys [description show-description tags]}]
  (let [show? (:show-description @(rf/subscribe [:settings]))]
    (when (and show? (seq description))
      [:div
       [layout/show-more-container show-description description
        #(rf/dispatch [(if @(rf/subscribe [:main-player/show])
                         :main-player/toggle-layout
                         :stream/toggle-layout)
                       :show-description])]
       [:div.flex.gap-2.py-2
        (for [[i tag] (map-indexed vector tags)]
          ^{:key i}
          [:span.bg-neutral-300.dark:bg-neutral-800.rounded-md.p-2.text-sm
           (str "#" tag)])]])))

(defn comments
  [{:keys [comments-page show-comments show-comments-loading] :as stream}]
  (let [show?         (:show-comments @(rf/subscribe [:settings]))
        service-color @(rf/subscribe [:service-color])]
    (when show?
      (if show-comments-loading
        [layout/loading-icon service-color "text-2xl"]
        (when (and show-comments comments-page)
          [comments/comments comments-page stream])))))

(defn related-items
  [_]
  (let [!menu-active? (r/atom nil)
        !layout       (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{:keys [related-streams]}]
      (let [show? (:show-related @(rf/subscribe [:settings]))]
        (when (and show? (seq related-streams))
          [:div
           [:div.flex.flex-wrap.items-center.justify-between.mt-8.min-w-full
            [:div.flex.gap-2.items-center
             [:span.font-semibold.text-xl "Next Up"]]
            [:div.flex.gap-x-4
             [layout/popover-menu !menu-active?
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [:queue/add-n
                                         related-streams
                                         true])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [:modals/open
                                         [modals/add-to-bookmark
                                          related-streams]])}]]
             [items/layout-switcher !layout]]
            [items/related-streams related-streams nil !layout]]])))))

(defn stream
  []
  (let [!active-tab (r/atom :comments)]
    (fn []
      (let [stream          @(rf/subscribe [:stream])
            !player         @(rf/subscribe [:main-player])
            page-loading?   @(rf/subscribe [:show-page-loading])
            comments-length (-> stream
                                :comments-page
                                :comments
                                count)]
        [:<>
         (when-not page-loading?
           [:div.flex.flex-col.justify-center.items-center.xl:pt-4
            [player/video-player stream !player]])
         [layout/content-container
          [metadata stream]
          [description stream]
          [:div.mt-10
           [layout/tabs
            [{:id       :comments
              :label    "Comments"
              :label-fn (fn [label]
                          [:div.flex.gap-3.items-center.justify-center
                           [:i.fa-solid.fa-comments]
                           [:span label]
                           [:span.dark:bg-neutral-800.rounded.px-2
                            comments-length]])}
             {:id       :related-items
              :label    "Related Items"
              :label-fn (fn [label]
                          [:div.flex.gap-3.items-center.justify-center
                           [:i.fa-solid.fa-table-list]
                           [:span label]])}]
            :selected-id @!active-tab
            :on-change #(reset! !active-tab %)]]
          (case @!active-tab
            :comments      [comments stream]
            :related-items [related-items stream]
            [comments stream])]]))))
