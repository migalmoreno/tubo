(ns tubo.components.items
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.layout :as layout]
   [tubo.components.modal :as modal]
   [tubo.components.modals.bookmarks :as bookmarks]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn item-content
  [{:keys [audio-streams video-streams type service-id bookmark-id url] :as item} item-route bookmarks]
  (let [!menu-active? (r/atom false)]
    (fn [{:keys [type service-id url name thumbnail-url description subscriber-count
                 stream-count verified? uploader-name uploader-url
                 uploader-avatar upload-date short-description view-count
                 duration audio-streams video-streams bookmark-id] :as item} item-route bookmarks]
      (let [stream? (or (= type "stream") audio-streams video-streams)
            liked?  (some #(= (:url %) url) (-> bookmarks first :items))
            items (if stream?
                    [{:label    "Add to queue"
                      :icon     [:i.fa-solid.fa-headphones]
                      :on-click #(rf/dispatch [::events/switch-to-audio-player item])}
                     {:label    (if liked? "Remove favorite" "Favorite")
                      :icon     (if liked?
                                  [:i.fa-solid.fa-heart {:style {:color (util/get-service-color service-id)}}]
                                  [:i.fa-regular.fa-heart])
                      :on-click #(rf/dispatch [(if liked? ::events/remove-from-likes ::events/add-to-likes) item])}
                     (if (some #(= (:url %) url) (:items (first (filter #(= (:id %) bookmark-id) bookmarks))))
                       {:label    "Remove from playlist"
                        :icon     [:i.fa-solid.fa-trash]
                        :on-click #(rf/dispatch [::events/remove-from-bookmark-list item])}
                       {:label    "Add to playlist"
                        :icon     [:i.fa-solid.fa-plus]
                        :on-click #(rf/dispatch [::events/add-bookmark-list-modal
                                                 [bookmarks/add-to-bookmark-list-modal item]])})]
                    [(when (and bookmarks (some #(= (:id %) bookmark-id) (rest bookmarks)))
                       {:label    "Remove playlist"
                        :icon     [:i.fa-solid.fa-trash]
                        :on-click #(rf/dispatch [::events/remove-bookmark-list bookmark-id])})])]
        [:<>
         (when name
           [:div.flex.items-center.my-2
            [:a {:href item-route :title name}
             [:h1.line-clamp-2.my-1 {:class "[overflow-wrap:anywhere]"} name]]
            (when (and verified? (not uploader-url))
              [:i.fa-solid.fa-circle-check.pl-2])])
         [:div.flex.justify-between
          [:div.flex.items-center.my-2
           (if uploader-url
             [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
              [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2.break-all
               {:class "[overflow-wrap:anywhere]"}
               uploader-name]]
             [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2 uploader-name])
           (when (and uploader-url verified?)
             [:i.fa-solid.fa-circle-check])]
          (when-not (empty? (remove nil? items))
            [layout/more-menu !menu-active? items])]
         (when (and subscriber-count (not stream?))
           [:div.flex.items-center
            [:i.fa-solid.fa-users.text-xs]
            [:p.mx-2 (util/format-quantity subscriber-count)]])
         (when stream-count
           [:div.flex.items-center
            [:i.fa-solid.fa-video.text-xs]
            [:p.mx-2 (util/format-quantity stream-count)]])
         [:div.flex.my-1.justify-between
          [:p (util/format-date-ago upload-date)]
          (when view-count
            [:div.flex.items-center.h-full.pl-2
             [:i.fa-solid.fa-eye.text-xs]
             [:p.pl-1.5 (util/format-quantity view-count)]])]]))))

(defn generic-item
  [{:keys [url name thumbnail-url duration] :as item} bookmarks]
  (let [item-url (case (:type item)
                   "stream"   (rfe/href :tubo.routes/stream nil {:url url})
                   "channel"  (rfe/href :tubo.routes/channel nil {:url url})
                   "playlist" (rfe/href :tubo.routes/playlist nil {:url url})
                   url)]
    [:div.w-full
     [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
      [layout/thumbnail thumbnail-url item-url name duration]
      [item-content item item-url bookmarks]]]))

(defn related-streams
  [related-streams next-page-url]
  (let [service-color @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        bookmarks @(rf/subscribe [:bookmarks])]
    [:div.flex.flex-col.items-center.flex-auto.my-2.md:my-8
     [modal/modal]
     (if (empty? related-streams)
       [:div.flex.items-center.flex-auto.flex-col.justify-center.gap-y-4
        [:i.fa-solid.fa-ghost.text-3xl]
        [:p.text-lg "No available streams"]]
       [:div.grid.w-full.gap-x-10.gap-y-6
        {:class "xs:grid-cols-[repeat(auto-fill,_minmax(165px,_1fr))]"}
        (for [[i item] (map-indexed vector related-streams)]
          ^{:key i} [generic-item item bookmarks])])
     (when-not (empty? next-page-url)
       [layout/loading-icon service-color "text-2xl" (when-not pagination-loading? "invisible")])]))
