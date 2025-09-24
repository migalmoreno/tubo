(ns tubo.items.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as bookmarks]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn item-popover
  [{:keys [audio-streams video-streams type url playlist-id uploader-url
           uploader-name uploader-avatars uploader-verified?]
    :as   item}]
  (let [items
        (cond
          (or (= type "stream") audio-streams video-streams)
          [{:label    "Add to queue"
            :icon     [:i.fa-solid.fa-headphones]
            :on-click #(rf/dispatch [:queue/add item true])}
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
           (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
             {:label    "Unsubscribe from channel"
              :icon     [:i.fa-solid.fa-user-minus]
              :on-click #(rf/dispatch [:subscriptions/remove uploader-url])}
             {:label    "Subscribe to channel"
              :icon     [:i.fa-solid.fa-user-plus]
              :on-click #(rf/dispatch [:subscriptions/add
                                       {:url       uploader-url
                                        :name      uploader-name
                                        :verified? uploader-verified?
                                        :avatars   uploader-avatars}])})
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigation/navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}]
          (= type "channel")
          [(if @(rf/subscribe [:subscriptions/subscribed url])
             {:label    "Unsubscribe"
              :icon     [:i.fa-solid.fa-user-minus]
              :on-click #(rf/dispatch [:subscriptions/remove url])}
             {:label    "Subscribe to channel"
              :icon     [:i.fa-solid.fa-user-plus]
              :on-click #(rf/dispatch [:subscriptions/add item])})]
          (= type "playlist")
          [{:label    "Add to queue"
            :icon     [:i.fa-solid.fa-headphones]
            :on-click #(rf/dispatch [:playlist/fetch-related-streams url])}]
          :else [(when @(rf/subscribe [:bookmarks/bookmarked playlist-id])
                   {:label    "Remove playlist"
                    :icon     [:i.fa-solid.fa-trash]
                    :on-click #(rf/dispatch [:bookmarks/remove playlist-id
                                             true])})])]
    (when (not-empty (remove nil? items))
      [layout/popover items
       :extra-classes [:pr-0 :pl-4]
       :tooltip-classes ["right-5" "top-0"]])))

(defn grid-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified? upload-date
           subscriber-count view-count stream-count verified? type]
    :as   item}]
  (let [route (case type
                "stream"   (rfe/href :stream-page nil {:url url})
                "channel"  (rfe/href :channel-page nil {:url url})
                "playlist" (rfe/href :playlist-page nil {:url url})
                url)]
    [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
     [layout/thumbnail item route :container-classes
      (if (= type "channel")
        ["h-36" "w-36" "m-auto"]
        ["py-2" "h-44" "xs:h-40"])
      :image-classes
      (if (= type "channel") ["rounded-full"] ["rounded-lg"])]
     [:div
      [:div.flex.justify-between.my-2
       (when name
         [:div.flex.gap-x-2.font-medium.text-sm
          [:a {:href route :title name}
           [:span.line-clamp-2
            {:class "[overflow-wrap:anywhere]"}
            name]]
          (when (and verified? (not uploader-url))
            [:i.fa-solid.fa-circle-check.text-sm])])
       [:div.h-fit
        [item-popover item]]]
      (when uploader-url
        [:div.flex.justify-between.text-neutral-600.dark:text-neutral-400.items-center.my-2.text-sm
         [:div.flex.gap-x-2.items-center
          [layout/uploader-avatar item :classes ["w-6" "h-6"]]
          (conj
           (when uploader-url
             [:a
              {:href  (rfe/href :channel-page nil {:url uploader-url})
               :title uploader-name
               :key   url}])
           [:span.line-clamp-1.break-all.text-xs
            {:class "[overflow-wrap:anywhere]" :title uploader-name :key url}
            uploader-name])
          (when (and uploader-url uploader-verified?)
            [:i.fa-solid.fa-circle-check.text-xs])]])
      [:div.text-neutral-600.dark:text-neutral-400.text-xs.flex.flex-col.gap-y-2
       (when (or subscriber-count stream-count)
         [:div.flex.gap-x-2
          (when (and (= type "channel") subscriber-count)
            [:span
             (str (utils/format-quantity subscriber-count) " subscribers")])
          (when (and (= type "channel") subscriber-count stream-count)
            [layout/bullet])
          (when (and (= type "channel") stream-count)
            [:span (str (utils/format-quantity stream-count) " streams")])])
       (when (or upload-date view-count)
         [:div.flex.items-center.my-1.gap-x-1
          (when upload-date
            [:span (utils/format-date-ago upload-date)])
          (when (and upload-date view-count)
            [layout/bullet])
          (when view-count
            [:span (str (utils/format-quantity view-count) " views")])])]]]))

(defn list-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified? description
           subscriber-count view-count stream-count verified? upload-date type]
    :as   item} &
   {:keys [author-classes container-classes metadata-classes title-classes
           thumbnail-container-classes thumbnail-image-classes]
    :or   {author-classes              ["[overflow-wrap:anywhere]"
                                        "line-clamp-1" "break-all" "text-xs"]
           container-classes           ["xs:mr-2"]
           metadata-classes            ["text-xs" "gap-y-3"]
           thumbnail-container-classes ["py-2" "h-28" "min-w-[150px]"
                                        "max-w-[150px]"
                                        "xs:h-32" "xs:min-w-[175px]"
                                        "xs:max-w-[175px]"
                                        "sm:h-44" "sm:min-w-[300px]"
                                        "sm:max-w-[300px]"
                                        "lg:min-w-[350px]" "lg:max-w-[350px]"
                                        "lg:h-48"]
           thumbnail-image-classes     ["rounded-lg"]
           title-classes               ["[overflow-wrap:anywhere]" "text-sm"
                                        "w-fit"
                                        "mt-2" "line-clamp-1" "sm:text-lg"]}}]
  (let [route (case (:type item)
                "stream"   (rfe/href :stream-page nil {:url url})
                "channel"  (rfe/href :channel-page nil {:url url})
                "playlist" (rfe/href :playlist-page nil {:url url})
                url)]
    [:div.flex.gap-x-3.xs:gap-x-5
     [layout/thumbnail item route
      :container-classes thumbnail-container-classes
      :image-classes
      (if (= type "channel")
        ["rounded-full" "!min-w-16" "m-auto"]
        thumbnail-image-classes)]
     [:div.flex.flex-col.flex-auto {:class container-classes}
      [:div.flex.items-center.justify-between
       (when name
         [:a {:href route :title name}
          [:div {:class title-classes}
           [:span name]
           (when (and verified? (not uploader-url))
             [:i.fa-solid.fa-circle-check.pl-3.text-sm.w-fit])]])
       [item-popover item]]
      [:div.flex.flex-col.justify-center.text-neutral-600.dark:text-neutral-400
       {:class metadata-classes}
       (when (or view-count upload-date)
         [:div.flex.items-center.gap-x-1
          (when view-count
            [:<>
             [:div.flex.items-center.h-full.whitespace-nowrap
              [:p {:class metadata-classes}
               (str (utils/format-quantity view-count) " views")]]
             (when upload-date
               [layout/bullet])])
          (when upload-date
            [:span.line-clamp-1 {:class metadata-classes}
             (utils/format-date-ago upload-date)])])
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
       (when (and (= type "channel") (or subscriber-count stream-count))
         [:div.flex.flex-col.xs:flex-row.gap-x-1
          (when (and (= type "channel") subscriber-count)
            [:<>
             [:div.flex.items-center.h-full
              [:p
               (str (utils/format-quantity subscriber-count) " subscribers")]]
             (when stream-count
               [layout/bullet])])
          (when stream-count
            [:span
             (str (utils/format-quantity stream-count) " streams")])])
       (when (seq description)
         [:span.text-xs.line-clamp-1.sm:line-clamp-2.leading-5.max-xs:hidden
          {:class "[overflow-wrap:anywhere]"}
          description])]]]))

(defn items-container
  []
  (let [!observer (atom nil)]
    (fn [related-streams next-page layout pagination-fn]
      [:<>
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
          (if (and layout (= layout "grid"))
            [grid-item-content item]
            [list-item-content item])])])))

(defn related-streams
  [related-streams next-page layout pagination-fn]
  (let [service-color       @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])]
    [:div.flex.flex-col.flex-auto.my-2.md:my-8
     (if (seq related-streams)
       (conj (if (and layout (= layout "grid"))
               [:div.grid.w-full.gap-x-4.gap-y-4
                {:class "xs:grid-cols-[repeat(auto-fill,_minmax(215px,_1fr))]"}]
               [:div.flex.flex-col.gap-x-10])
             [items-container related-streams next-page layout pagination-fn])
       [:div.flex.items-center.flex-auto.flex-col.justify-center
        [:span "No available items"]])
     (when (and pagination-loading? (seq next-page))
       [layout/loading-icon service-color :text-md])]))

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
