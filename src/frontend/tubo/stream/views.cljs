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
  [{:keys [url related-streams] :as stream}]
  [layout/popover
   (into
    [{:label    "Add to queue"
      :icon     [:i.fa-solid.fa-headphones]
      :on-click #(rf/dispatch [:bg-player/show stream true])}
     {:label    "Start radio"
      :icon     [:i.fa-solid.fa-tower-cell]
      :on-click #(rf/dispatch [:bg-player/start-radio stream])}
     {:label "Original"
      :link  {:route url :external? true}
      :icon  [:i.fa-solid.fa-external-link-alt]}
     {:label    "Add to playlist"
      :icon     [:i.fa-solid.fa-plus]
      :on-click #(rf/dispatch [:modals/open
                               [modals/add-to-bookmark stream]])}]
    (when (seq related-streams)
      [{:label    "Add related to queue"
        :icon     [:i.fa-solid.fa-headphones]
        :on-click #(rf/dispatch [:queue/add-n
                                 related-streams
                                 true])}
       {:label    "Add related to playlist"
        :icon     [:i.fa-solid.fa-plus]
        :on-click #(rf/dispatch [:modals/open
                                 [modals/add-to-bookmark
                                  related-streams]])}]))
   :tooltip-classes ["right-5" "top-5"]
   :extra-classes ["p-3" "xs:py-2" "xs:px-4"]])

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
      [:div.flex.items-center.text-neutral-600.dark:text-neutral-400.text-xs.sm:text-sm
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
  [:div
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
  [{:keys [related-streams]}]
  (let [show? (:show-related @(rf/subscribe [:settings]))]
    (when (and show? (seq related-streams))
      [:div.flex.flex-col.min-w-fit.flex-auto
       [:div.flex.flex-col.gap-x-10.gap-y-4
        (for [[i item] (map-indexed vector related-streams)]
          ^{:key i}
          [items/list-item-content item
           :author-classes ["line-clamp-1" "text-xs"]
           :title-classes ["font-semibold" "line-clamp-2" "text-xs"]
           :metadata-classes ["text-xs"]
           :thumbnail-classes
           ["h-[5.5rem]" "min-w-[150px]" "max-w-[150px]"]])]])))

(defn video-container
  []
  (let [!active-tab (r/atom :comments)]
    (fn [stream video]
      [:div.flex.flex-col.w-full.flex-1
       [:div.flex.flex-col.justify-center.items-center.sticky.md:static.z-10
        {:class ["top-[56px]"]}
        video]
       [:div.flex.flex-col.py-4.w-full.px-4.md:px-0
        [metadata stream]
        [:div.hidden.lg:flex.flex-col.gap-y-8.py-4
         [description stream]
         [:div
          [:h1.text-2xl.font-bold.pb-2 "Comments"]
          [comments stream]]]
        [:div.fixed.right-0.bg-neutral-100.dark:bg-neutral-950.z-10.w-full.md:hidden
         {:class
          (if @(rf/subscribe [:bg-player/show])
            "!bottom-[80px]"
            "!bottom-0")}
         [layout/tabs
          [{:id        :comments
            :label     "Comments"
            :left-icon [:i.fa-solid.fa-comments]}
           {:id        :related-items
            :label     "Related Items"
            :left-icon [:i.fa-solid.fa-images]}
           {:id        :description
            :label     "Description"
            :left-icon [:i.fa-solid.fa-file]}]
          :selected-id @!active-tab
          :on-change #(reset! !active-tab %)]]
        [:div.hidden.md:flex.flex-col.lg:hidden.gap-y-8
         [description stream]
         [related-items stream]
         [:div
          [:h1.text-2xl.font-bold.pb-2 "Comments"]
          [comments stream]]]
        [:div.mt-4.mb-16.md:hidden
         (case @!active-tab
           :comments      [comments stream]
           :related-items [related-items stream]
           :description   [description stream])]]])))

(defn stream
  []
  (let [stream        @(rf/subscribe [:stream])
        !player       @(rf/subscribe [:main-player])
        page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    (if page-loading?
      [layout/loading-icon service-color :text-5xl]
      [:div.flex.flex-col.flex-auto.items-center.md:my-4
       [:div.flex.gap-x-6.w-full {:class ["md:w-[95%] xl:w-11/12"]}
        [video-container stream
         [player/video-player stream !player {}
          #(rf/dispatch [:player/set-stream stream !player])]]
        [:div.hidden.lg:block {:class "max-w-[350px]"}
         [related-items stream]]]])))
