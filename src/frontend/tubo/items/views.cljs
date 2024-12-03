(ns tubo.items.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as bookmarks]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn item-popover
  [_ _]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [service-id audio-streams video-streams type url bookmark-id
                 uploader-url]
          :as   item} bookmarks]
      (let [liked? (some #(= (:url %) url)
                         (-> bookmarks
                             first
                             :items))
            items
            (if (or (= type "stream") audio-streams video-streams)
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [:bg-player/show item true])}
               {:label    "Start radio"
                :icon     [:i.fa-solid.fa-tower-cell]
                :on-click #(rf/dispatch [:bg-player/start-radio item])}
               {:label    (if liked? "Remove favorite" "Favorite")
                :icon     [:i.fa-solid.fa-heart
                           (when (and liked? service-id)
                             {:style {:color (utils/get-service-color
                                              service-id)}})]
                :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add)
                                         item true])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [:modals/open
                                         [bookmarks/add-to-bookmark item]])}
               (when (some #(= (:url %) url)
                           (:items (first (filter #(= (:id %) bookmark-id)
                                                  bookmarks))))
                 {:label    "Remove from playlist"
                  :icon     [:i.fa-solid.fa-trash]
                  :on-click #(rf/dispatch [:bookmark/remove item])})
               {:label    "Show channel details"
                :icon     [:i.fa-solid.fa-user]
                :on-click #(rf/dispatch [:navigation/navigate
                                         {:name   :channel-page
                                          :params {}
                                          :query  {:url uploader-url}}])}]
              [(when (and bookmarks
                          (some #(= (:id %) bookmark-id) (rest bookmarks)))
                 {:label    "Remove playlist"
                  :icon     [:i.fa-solid.fa-trash]
                  :on-click #(rf/dispatch [:bookmarks/remove bookmark-id
                                           true])})])]
        (when (not-empty (remove nil? items))
          [layout/popover-menu !menu-active? items :extra-classes
           [:pr-0 :pl-4] :menu-styles {:right "15px"}])))))

(defn grid-item-content
  [{:keys [url name uploader-url uploader-name subscriber-count view-count
           stream-count verified? thumbnails duration type]
    :as   item} route bookmarks]
  [:div.w-full
   [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
    [layout/thumbnail
     (-> thumbnails
         last
         :url) route name duration
     :classes [:py-2 :h-44 "xs:h-28"] :rounded? true]
    [:div
     (when name
       [:div.flex.items-center.my-2
        [:a {:href route :title name}
         [:h1.line-clamp-2.my-1 {:class "[overflow-wrap:anywhere]"} name]]
        (when (and verified? (not uploader-url))
          [:i.fa-solid.fa-circle-check.pl-2.text-sm])])
     [:div.flex.justify-between
      [:div.flex.items-center.my-2
       (conj
        (when uploader-url
          [:a
           {:href  (rfe/href :channel-page nil {:url uploader-url})
            :title uploader-name
            :key   url}])
        [:h1.text-neutral-800.dark:text-gray-300.font-semibold.pr-2.line-clamp-1.break-all
         {:class "[overflow-wrap:anywhere]" :title uploader-name :key url}
         uploader-name])
       (when (and uploader-url verified?)
         [:i.fa-solid.fa-circle-check.text-xs])]
      [item-popover item bookmarks]]
     (when (and (= type "channel") subscriber-count)
       [:div.flex.items-center
        [:i.fa-solid.fa-users.text-xs]
        [:span.mx-2 (utils/format-quantity subscriber-count)]])
     (when stream-count
       [:div.flex.items-center
        [:i.fa-solid.fa-video.text-xs]
        [:span.mx-2 (utils/format-quantity stream-count)]])
     [:div.flex.my-1.justify-between
      [:span (utils/format-date-ago (:upload-date item))]
      (when view-count
        [:div.flex.items-center.h-full.pl-2
         [:i.fa-solid.fa-eye.text-xs]
         [:p.pl-1.5 (utils/format-quantity view-count)]])]]]])

(defn list-item-content
  [{:keys [url name uploader-url uploader-name subscriber-count view-count
           stream-count verified? thumbnails duration upload-date type]
    :as   item} route bookmarks]
  [:div.w-full
   [:div.flex.gap-x-3.xs:gap-x-5
    [layout/thumbnail
     (-> thumbnails
         last
         :url) route name duration
     :classes
     [:py-2 :h-28 "sm:h-36" "min-w-[130px]" "max-w-[130px]" "sm:min-w-[250px]"
      "sm:max-w-[250px]"] :rounded?
     true]
    [:div.flex.flex-col.flex-auto.xs:mr-2.gap-y-2
     (when name
       [:div.flex.items-center.justify-between.mt-2
        [:a {:href route :title name}
         [:h1.line-clamp-1.xs:text-xl
          {:class "[overflow-wrap:anywhere]"}
          name
          (when (and verified? (not uploader-url))
            [:i.fa-solid.fa-circle-check.pl-3.text-sm])]]
        [item-popover item bookmarks]])
     [:div.flex.items-center.justify-between.gap-y-2
      [:div.flex.flex-col.justify-center.gap-y-2.text-neutral-600.dark:text-neutral-400
       (when (or uploader-url uploader-name)
         [:div.flex.gap-2.items-center
          [layout/uploader-avatar item :classes ["w-6" "h-6"]]
          (conj
           (when uploader-url
             [:a
              {:href  (rfe/href :channel-page nil {:url uploader-url})
               :title uploader-name
               :key   url}])
           [:h1.font-semibold.line-clamp-1.break-all.text-sm
            {:class "[overflow-wrap:anywhere]" :title uploader-name :key url}
            uploader-name])
          (when (and uploader-url verified?)
            [:i.fa-solid.fa-circle-check.text-xs])])
       (when (or view-count upload-date)
         [:div.flex.text-xs.xs:text-sm.flex-col.xs:flex-row
          (when view-count
            [:<>
             [:div.flex.items-center.h-full
              [:p (str (utils/format-quantity view-count) " views")]]
             [:span.px-2.hidden.xs:inline-block
              {:dangerouslySetInnerHTML {:__html "&bull;"}}]])
          [:span (utils/format-date-ago upload-date)]])
       (when (or subscriber-count stream-count)
         [:div.flex.text-xs.xs:text-sm.flex-col.xs:flex-row
          (when (and (= type "channel") subscriber-count)
            [:<>
             [:div.flex.items-center.h-full
              [:p
               (str (utils/format-quantity subscriber-count) " subscribers")]]
             (when stream-count
               [:span.px-2.hidden.xs:inline-block
                {:dangerouslySetInnerHTML {:__html "&bull;"}}])])
          (when stream-count
            [:span
             (str (utils/format-quantity stream-count) " streams")])])]]]]])

(defn related-streams
  [related-streams next-page-url !layout]
  (let [service-color       @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        bookmarks           @(rf/subscribe [:bookmarks])
        item-url            #(case (:type %)
                               "stream"   (rfe/href :stream-page
                                                    nil
                                                    {:url (:url %)})
                               "channel"  (rfe/href :channel-page
                                                    nil
                                                    {:url (:url %)})
                               "playlist" (rfe/href :playlist-page
                                                    nil
                                                    {:url (:url %)})
                               (:url %))]
    [:div.flex.flex-col.flex-auto.my-2.md:my-8
     (if (empty? related-streams)
       [:div.flex.items-center.flex-auto.flex-col.justify-center.gap-y-4
        [:i.fa-solid.fa-ghost.text-3xl]
        [:p.text-lg "No available streams"]]
       (if (and !layout (= @!layout "grid"))
         [:div.grid.w-full.gap-x-10.gap-y-6
          {:class "xs:grid-cols-[repeat(auto-fill,_minmax(165px,_1fr))]"}
          (for [[i item] (map-indexed vector related-streams)]
            ^{:key i} [grid-item-content item (item-url item) bookmarks])]
         [:div.flex.flex-wrap.w-full.gap-x-10.gap-y-6
          (for [[i item] (map-indexed vector related-streams)]
            ^{:key i} [list-item-content item (item-url item) bookmarks])]))
     (when (and pagination-loading? (seq next-page-url))
       [layout/loading-icon service-color :text-md])]))

(defn layout-switcher
  [!layout]
  [:div.gap-x-5.text-lg.flex.items-center.justify-end
   [:button.flex.items-center
    {:on-click #(reset! !layout "list")
     :title    "Switch to list layout"}
    [:i.fa-solid.fa-list.text-sm
     {:class (when-not (= @!layout "list")
               ["dark:text-neutral-500" "text-neutral-400"])}]]
   [:button.flex.items-center
    {:on-click #(reset! !layout "grid")
     :title    "Switch to grid layout"}
    [:i.fa-solid.fa-grip.text-base
     {:class (when-not (= @!layout "grid")
               ["dark:text-neutral-500" "text-neutral-400"])}]]])
