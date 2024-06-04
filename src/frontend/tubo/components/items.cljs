(ns tubo.components.items
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as bookmarks]
   [tubo.components.layout :as layout]
   [tubo.modals.views :as modals]
   [tubo.utils :as utils]))

(defn item-popover
  [_ _]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [service-id audio-streams video-streams type url bookmark-id uploader-url] :as item} bookmarks]
      (let [liked?  (some #(= (:url %) url) (-> bookmarks first :items))
            items (if (or (= type "stream") audio-streams video-streams)
                    [{:label    "Add to queue"
                      :icon     [:i.fa-solid.fa-headphones]
                      :on-click #(rf/dispatch [:player/switch-to-background item])}
                     {:label    "Play radio"
                      :icon     [:i.fa-solid.fa-tower-cell]
                      :on-click #(rf/dispatch [:player/start-radio item])}
                     {:label    (if liked? "Remove favorite" "Favorite")
                      :icon     [:i.fa-solid.fa-heart (when (and liked? service-id)
                                                        {:style {:color (utils/get-service-color service-id)}})]
                      :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add) item true])}
                     {:label    "Add to playlist"
                      :icon     [:i.fa-solid.fa-plus]
                      :on-click #(rf/dispatch [:modals/open [bookmarks/add-to-bookmark item]])}
                     (when (some #(= (:url %) url) (:items (first (filter #(= (:id %) bookmark-id) bookmarks))))
                       {:label    "Remove from playlist"
                        :icon     [:i.fa-solid.fa-trash]
                        :on-click #(rf/dispatch [:bookmark/remove item])})
                     {:label    "Show channel details"
                      :icon     [:i.fa-solid.fa-user]
                      :on-click #(rf/dispatch [:navigate
                                               {:name   :channel-page
                                                :params {}
                                                :query  {:url uploader-url}}])}]
                    [(when (and bookmarks (some #(= (:id %) bookmark-id) (rest bookmarks)))
                       {:label    "Remove playlist"
                        :icon     [:i.fa-solid.fa-trash]
                        :on-click #(rf/dispatch [:bookmarks/remove bookmark-id true])})])]
        (when (not-empty (remove nil? items))
          [layout/popover-menu !menu-active? items])))))

(defn item-content
  [{:keys [url name uploader-url uploader-name subscriber-count view-count stream-count verified?] :as item} route bookmarks]
  [:div
   (when name
     [:div.flex.items-center.my-2
      [:a {:href route :title name}
       [:h1.line-clamp-2.my-1 {:class "[overflow-wrap:anywhere]"} name]]
      (when (and verified? (not uploader-url))
        [:i.fa-solid.fa-circle-check.pl-2])])
   [:div.flex.justify-between
    [:div.flex.items-center.my-2
     (conj
      (when uploader-url
        [:a {:href  (rfe/href :channel-page nil {:url uploader-url})
             :title uploader-name
             :key url}])
      [:h1.text-neutral-800.dark:text-gray-300.font-semibold.pr-2.line-clamp-1.break-all
       {:class "[overflow-wrap:anywhere]" :title uploader-name :key url}
       uploader-name])
     (when (and uploader-url verified?)
       [:i.fa-solid.fa-circle-check])]
    [item-popover item bookmarks]]
   (when subscriber-count
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
       [:p.pl-1.5 (utils/format-quantity view-count)]])]])

(defn generic-item
  [{:keys [url name thumbnail-url duration] :as item} bookmarks]
  (let [item-url (case (:type item)
                   "stream"   (rfe/href :stream-page nil {:url url})
                   "channel"  (rfe/href :channel-page nil {:url url})
                   "playlist" (rfe/href :playlist-page nil {:url url})
                   url)]
    [:div.w-full
     [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
      [layout/thumbnail thumbnail-url item-url name duration
       :classes [:py-2 :h-44 "xs:h-28"] :rounded? true]
      [item-content item item-url bookmarks]]]))

(defn related-streams
  [related-streams next-page-url]
  (let [service-color       @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        bookmarks           @(rf/subscribe [:bookmarks])]
    [:div.flex.flex-col.items-center.flex-auto.my-2.md:my-8
     [modals/modal]
     (if (empty? related-streams)
       [:div.flex.items-center.flex-auto.flex-col.justify-center.gap-y-4
        [:i.fa-solid.fa-ghost.text-3xl]
        [:p.text-lg "No available streams"]]
       [:div.grid.w-full.gap-x-10.gap-y-6
        {:class "xs:grid-cols-[repeat(auto-fill,_minmax(165px,_1fr))]"}
        (for [[i item] (map-indexed vector related-streams)]
          ^{:key i} [generic-item item bookmarks])])
     (when (and pagination-loading? (not (empty? next-page-url)))
       [layout/loading-icon service-color :text-md])]))
