(ns tubo.items.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as bookmarks]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn item-popover
  [{:keys [audio-streams video-streams type url playlist-id uploader-url]
    :as   item}]
  (let [items
        (if (or (= type "stream") audio-streams video-streams)
          [{:label    "Add to queue"
            :icon     [:i.fa-solid.fa-headphones]
            :on-click #(rf/dispatch [:bg-player/show item true])}
           {:label    "Start radio"
            :icon     [:i.fa-solid.fa-tower-cell]
            :on-click #(rf/dispatch [:bg-player/start-radio item])}
           {:label    "Add to playlist"
            :icon     [:i.fa-solid.fa-plus]
            :on-click #(rf/dispatch [:modals/open
                                     [bookmarks/add-to-bookmark item]])}
           (when @(rf/subscribe [:bookmarks/playlisted url playlist-id])
             {:label    "Remove from playlist"
              :icon     [:i.fa-solid.fa-trash]
              :on-click #(rf/dispatch [:bookmark/remove item])})
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigation/navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}]
          [(when @(rf/subscribe [:bookmarks/bookmarked playlist-id])
             {:label    "Remove playlist"
              :icon     [:i.fa-solid.fa-trash]
              :on-click #(rf/dispatch [:bookmarks/remove playlist-id
                                       true])})])]
    (when (not-empty (remove nil? items))
      [layout/popover items
       :extra-classes [:pr-0 :pl-4]
       :tooltip-classes ["right-5" "top-0"]])))

(defn grid-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified?
           subscriber-count view-count stream-count verified? type]
    :as   item}]
  (let [route (case (:type item)
                "stream"   (rfe/href :stream-page
                                     nil
                                     {:url (:url item)})
                "channel"  (rfe/href :channel-page
                                     nil
                                     {:url (:url item)})
                "playlist" (rfe/href :playlist-page
                                     nil
                                     {:url (:url item)})
                (:url item))]
    [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
     [layout/thumbnail item route :classes [:py-2 :h-44 "xs:h-28"] :rounded?
      true]
     [:div
      (when name
        [:div.flex.items-center.my-2
         [:a {:href route :title name}
          [:h1.line-clamp-2.my-1.font-semibold
           {:class "[overflow-wrap:anywhere]"}
           name]]
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
        (when (and uploader-url uploader-verified?)
          [:i.fa-solid.fa-circle-check.text-xs])]
       [item-popover item]]
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
          [:p.pl-1.5 (utils/format-quantity view-count)]])]]]))

(defn list-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified?
           subscriber-count view-count stream-count verified? upload-date type]
    :as   item} &
   {:keys [author-classes title-classes thumbnail-classes metadata-classes]
    :or   {thumbnail-classes ["py-2" "h-24" "min-w-[125px]" "max-w-[125px]"
                              "sm:h-36" "sm:min-w-[250px]" "sm:max-w-[250px]"]
           title-classes     ["[overflow-wrap:anywhere]" "line-clamp-1"
                              "text-sm" "sm:text-xl" "mt-2" "font-semibold"]
           author-classes    ["[overflow-wrap:anywhere]" "font-semibold"
                              "line-clamp-1" "break-all" "text-xs"
                              "xs:text-sm"]
           metadata-classes  ["text-xs" "xs:text-sm"]}}]
  (let [route (case (:type item)
                "stream"   (rfe/href :stream-page
                                     nil
                                     {:url (:url item)})
                "channel"  (rfe/href :channel-page
                                     nil
                                     {:url (:url item)})
                "playlist" (rfe/href :playlist-page
                                     nil
                                     {:url (:url item)})
                (:url item))]
    [:div.flex.gap-x-3.xs:gap-x-5
     [layout/thumbnail item route :classes thumbnail-classes
      :rounded? true]
     [:div.flex.flex-col.flex-auto.xs:mr-2.gap-y-2
      [:div.flex.items-center.justify-between
       (when name
         [:a {:href route :title name}
          [:h1 {:class title-classes} name
           (when (and verified? (not uploader-url))
             [:i.fa-solid.fa-circle-check.pl-3.text-sm])]])
       [item-popover item]]
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
            [:h1
             {:class author-classes :title uploader-name :key url}
             uploader-name])
           (when (and uploader-url uploader-verified?)
             [:i.fa-solid.fa-circle-check.text-xs])])
        (when (or view-count upload-date)
          [:div.flex.items-center
           (when view-count
             [:<>
              [:div.flex.items-center.h-full.whitespace-nowrap
               [:p {:class metadata-classes}
                (str (utils/format-quantity view-count) " views")]]
              (when upload-date
                [:span.px-2
                 {:dangerouslySetInnerHTML {:__html "&bull;"}
                  :style                   {:font-size "0.5rem"}}])])
           (when upload-date
             [:span.line-clamp-1 {:class metadata-classes}
              (utils/format-date-ago upload-date)])])
        (when (or subscriber-count stream-count)
          [:div.flex.flex-col.xs:flex-row
           {:class metadata-classes}
           (when (and (= type "channel") subscriber-count)
             [:<>
              [:div.flex.items-center.h-full
               [:p
                (str (utils/format-quantity subscriber-count) " subscribers")]]
              (when stream-count
                [:span.px-2.hidden.xs:inline-block
                 {:dangerouslySetInnerHTML {:__html "&bull;"}
                  :style                   {:font-size "0.5rem"}}])])
           (when stream-count
             [:span
              (str (utils/format-quantity stream-count) " streams")])])]]]]))

(defn related-streams
  []
  (let [!observer (atom nil)]
    (fn [related-streams next-page !layout pagination-fn]
      (let [service-color       @(rf/subscribe [:service-color])
            pagination-loading? @(rf/subscribe [:show-pagination-loading])
            items               (doall
                                 (for [[i item]
                                       (map-indexed vector related-streams)]
                                   ^{:key i}
                                   [:div
                                    {:ref
                                     (when (and (seq next-page)
                                                (= (+ i 1)
                                                   (count related-streams)))
                                       #(rf/dispatch
                                         [:layout/add-intersection-observer
                                          !observer
                                          %
                                          (fn [entries]
                                            (when (.-isIntersecting (first
                                                                     entries))
                                              (pagination-fn)))]))}
                                    (if (and !layout (= @!layout "grid"))
                                      [grid-item-content item]
                                      [list-item-content item])]))]
        [:div.flex.flex-col.flex-auto.my-2.md:my-8
         (if (empty? related-streams)
           [:div.flex.items-center.flex-auto.flex-col.justify-center
            [:span "No available streams"]]
           (if (and !layout (= @!layout "grid"))
             [:div.grid.w-full.gap-x-10.gap-y-4
              {:class "xs:grid-cols-[repeat(auto-fill,_minmax(165px,_1fr))]"}
              items]
             [:div.flex.flex-col.gap-x-10
              items]))
         (when (and pagination-loading? (seq next-page))
           [layout/loading-icon service-color :text-md])]))))

(defn layout-switcher
  [!layout]
  [layout/popover
   [{:label    "List"
     :icon     [:i.fa-solid.fa-table-list]
     :on-click #(reset! !layout "list")}
    {:label    "Grid"
     :icon     [:i.fa-solid.fa-table-cells-large]
     :on-click #(reset! !layout "grid")}]
   :responsive? false
   :extra-classes ["p-0"]
   :tooltip-classes ["right-3" "top-10"]
   :icon
   [:div.flex.items-center.gap-x-3.rounded-full.transition-all.ease-in-out.text-sm.text-neutral-500
    [:i.fa-solid
     {:class (if (= @!layout "list") :fa-table-list :fa-table-cells-large)}]
    [:i.fa-solid.fa-angle-down.text-sm]]])
