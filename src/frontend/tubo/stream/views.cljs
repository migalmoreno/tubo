(ns tubo.stream.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bg-player.views :as bg-player]
   [tubo.bookmarks.modals :as modals]
   [tubo.comments.views :as comments]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.queue.views :as queue]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defn metadata-popover
  [{:keys [url related-streams] :as stream}]
  [layout/popover
   (into
    [{:label    "Add to queue"
      :icon     [:i.fa-solid.fa-headphones]
      :on-click #(rf/dispatch [:queue/add stream true])}
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
   :extra-classes ["px-5" "xs:py-2.5" "xs:px-2.5" "rounded-full"]
   :icon
   [:<>
    [:i.fa-solid.fa-ellipsis-vertical.xs:hidden]
    [:i.fa-solid.fa-ellipsis.hidden.xs:block]]])

(defn metadata-uploader
  [{:keys [uploader-url uploader-name uploader-verified? subscriber-count
           uploader-avatars]
    :as   stream}]
  [:div.flex.items-center.justify-between.xs:justify-start.flex-auto.xs:flex-none.flex-wrap.xs:flex-nowrap.gap-4
   [:div.flex.items-center.gap-x-3
    [layout/uploader-avatar stream :classes ["w-12" "h-12"]]
    [:div.gap-x-2
     (when uploader-url
       [:div.flex.gap-x-2.items-center
        [:a.line-clamp-1.font-medium.text-sm.xs:text-base
         {:href  (rfe/href :channel-page nil {:url uploader-url})
          :title uploader-name}
         uploader-name]
        (when uploader-verified?
          [:i.fa-solid.fa-circle-check.text-xs.text-neutral-500])])
     (when subscriber-count
       [:div.flex.items-center.text-neutral-600.dark:text-neutral-400
        [:span {:title subscriber-count :class "text-[0.8rem]"}
         (str (utils/format-quantity subscriber-count) " subscribers")]])]]
   (when uploader-url
     (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
       [layout/secondary-button "Unsubscribe"
        #(rf/dispatch [:subscriptions/remove uploader-url])]
       [layout/primary-button "Subscribe"
        #(rf/dispatch [:subscriptions/add
                       {:url       uploader-url
                        :name      uploader-name
                        :verified? uploader-verified?
                        :avatars   uploader-avatars}])]))])

(defn metadata-stats
  [{:keys [like-count dislike-count] :as stream}]
  [:div.flex.items-center.justify-end.gap-x-2
   (when (or like-count dislike-count)
     [:div.flex.bg-neutral-200.dark:bg-neutral-900.px-4.py-2.rounded-full.text-sm.gap-x-4.font-medium
      (when like-count
        [:div.flex.items-center.gap-x-2
         [:i.fa-solid.fa-thumbs-up]
         [:span.text-neutral-800.dark:text-neutral-300
          (utils/format-quantity like-count)]])
      (when dislike-count
        [:div.flex.items-center.gap-x-2
         [:i.fa-solid.fa-thumbs-down]
         [:span.text-neutral-800.dark:text-neutral-300
          (utils/format-quantity dislike-count)]])])
   (when stream
     [:div.hidden.xs:flex.bg-neutral-200.dark:bg-neutral-900.rounded-full
      [metadata-popover stream]])])

(defn metadata
  [{:keys [name view-count upload-date] :as stream}]
  [:div
   (when name
     [:div.flex.items-center.justify-between
      [:h1.text-lg.sm:text-xl.font-semibold.line-clamp-2 {:title name} name]])
   [:div.flex.gap-x-2.text-neutral-600.dark:text-neutral-400.text-xs.sm:text-sm.my-1.items-center
    (when view-count
      [:span.whitespace-nowrap {:title view-count}
       (str (utils/format-quantity view-count) " views")])
    (when (and view-count upload-date)
      [layout/bullet])
    (when upload-date
      [:span.whitespace-nowrap {:title upload-date}
       (utils/format-date-ago upload-date)])]
   [:div.flex.justify-between.py-4.gap-x-2.gap-y-4.flex-wrap.xs:flex-nowrap
    [metadata-uploader stream]
    [metadata-stats stream]]])

(defn more-metadata
  [{:keys [tags license category privacy]}]
  [:div.flex.flex-col.py-4.gap-y-4.text-sm
   (when (seq category)
     [:div.flex
      [:label.font-bold.min-w-24.whitespace-nowrap "CATEGORY"]
      [:span category]])
   (when (seq license)
     [:div.flex
      [:label.font-bold.min-w-24.whitespace-nowrap "LICENSE"]
      [:span license]])
   (when (seq privacy)
     [:div.flex
      [:label.font-bold.min-w-24.whitespace-nowrap "PRIVACY"]
      [:span (str/capitalize privacy)]])
   (when (seq tags)
     [:div.flex
      [:label.font-bold.min-w-24.whitespace-nowrap "TAGS"]
      [:div.flex.gap-x-1.items-center.flex-wrap
       (for [[i tag] (map-indexed vector tags)]
         ^{:key i}
         [:span {:class "[overflow-wrap:anywhere]"} (str "#" tag)])]])])

(defn description
  [{:keys [description show-description] :as stream}]
  (let [show? (:show-description @(rf/subscribe [:settings]))]
    (when (and show? (seq description))
      [:div.rounded-lg.break-words.flex.flex-col.gap-y-8.text-sm
       {:class "[overflow-wrap:anywhere]"}
       [layout/show-more-container show-description
        description
        #(rf/dispatch [(if @(rf/subscribe [:main-player/show])
                         :main-player/toggle-layout
                         :stream/toggle-layout)
                       :show-description])]
       [more-metadata stream]])))

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
       [:div.flex.flex-col.gap-x-10.gap-y-2
        (for [[i item] (map-indexed vector related-streams)]
          ^{:key i}
          [items/list-item-content item
           :author-classes ["line-clamp-1" "text-xs"]
           :container-classes ["gap-y-1"]
           :metadata-classes ["text-xs" "gap-y-2"]
           :thumbnail-container-classes
           ["h-[5.5rem]" "min-w-[150px]" "max-w-[150px]" "rounded-full"]
           :thumbnail-image-classes ["rounded-lg"]
           :title-classes ["font-medium" "line-clamp-2" "text-xs"]])]])))

(defn stream-queue
  []
  (let [stream        @(rf/subscribe [:queue/current])
        pos           @(rf/subscribe [:queue/position])
        queue         @(rf/subscribe [:queue])
        loop-playback @(rf/subscribe [:player/loop])
        color         (utils/get-service-color (:service-id stream))
        shuffled      @(rf/subscribe [:player/shuffled])]
    (when @(rf/subscribe [:main-player/show])
      [:div.flex-col.w-full.rounded-lg
       [:div.bg-neutral-200.dark:bg-neutral-900.rounded-lg.border-neutral-700
        [:div.p-4.flex.items-center.justify-between.rounded-lg
         [:div.flex.flex-col
          [:h4.font-bold.text-lg "Queue"]
          [:span.text-xs.text-neutral-600.dark:text-neutral-500
           (str (inc pos) "/" (count queue))]]
         [:div.flex.items-center
          [:div.px-4
           [player/loop-button loop-playback color true]]
          [:div.pl-4.pr-5
           [player/shuffle-button shuffled color true]]
          [bg-player/popover stream :tooltip-classes ["top-0" "right-0"]]]]
        [:div.flex.flex-col.gap-y-1.w-full.h-64.max-h-64.overflow-y-auto.relative.scroll-smooth.scrollbar-none
         {:class "@container"}
         [queue/virtualized-queue]]]])))

(defn video-container
  []
  (let [!active-tab (r/atom :comments)]
    (fn [{:keys [comments-page] :as stream} video]
      (let [{:keys [show-comments show-related show-description]}
            @(rf/subscribe [:settings])
            show-main-player? @(rf/subscribe [:main-player/show])
            comments-container (when stream
                                 [:div.flex.flex-col.gap-y-4
                                  [:h1.text-2xl.font-bold.pb-2
                                   (let [{:keys [comments-count]} comments-page]
                                     (if (> comments-count 0)
                                       (str comments-count " comments")
                                       "Comments"))]
                                  [comments stream]])
            breakpoint @(rf/subscribe [:layout/breakpoint])]
        [:div.flex.flex-col.flex-1
         [:div.flex.flex-col.justify-center.items-center.sticky.md:static.z-10.relative
          {:class (if show-main-player? "top-0" "top-[56px]")}
          video]
         (when stream
           [:div.flex.flex-col.py-4.w-full.px-4.md:px-0
            [:div.flex.flex-col.gap-y-8
             (when (and show-main-player? (not= breakpoint :lg)) [stream-queue])
             [metadata stream]]
            [:div.hidden.lg:flex.flex-col.gap-y-8.py-4
             (when show-description [description stream])
             (when show-comments comments-container)]
            [:div.bg-neutral-100.dark:bg-neutral-950.md:hidden.-mx-4.md:mx-0.border-b.border-neutral-300.dark:border-neutral-700
             [layout/tabs
              [(when show-comments
                 {:id        :comments
                  :label     "Comments"
                  :left-icon [:i.fa-solid.fa-comments]})
               (when show-related
                 {:id        :related-items
                  :label     "Related Items"
                  :left-icon [:i.fa-solid.fa-images]})
               (when show-description
                 {:id        :description
                  :label     "Description"
                  :left-icon [:i.fa-solid.fa-file]})]
              :selected-id @!active-tab
              :on-change #(reset! !active-tab %)]]
            [:div.hidden.md:flex.flex-col.lg:hidden.gap-y-8
             (when show-description [description stream])
             (when show-related [related-items stream])
             (when show-comments comments-container)]
            [:div.mt-4.md:mb-16.md:hidden
             (case @!active-tab
               :comments      [comments stream]
               :related-items [related-items stream]
               :description   [description stream])]])]))))

(defn stream
  [video-stream video-container]
  (let [breakpoint @(rf/subscribe [:layout/breakpoint])]
    [:div
     {:class ["flex" "flex-col" "flex-auto" "items-center" "md:my-4"]}
     [:div.flex.gap-x-6.w-full {:class "md:w-[95%] xl:w-11/12"}
      video-container
      [:div.hidden.lg:flex.gap-y-6.flex-col.shrink-0.flex-1
       {:class "lg:min-w-[315px] lg:max-w-[350px]"}
       (when (= breakpoint :lg) [stream-queue])
       [related-items video-stream]]]]))

(defn stream-page
  []
  (let [!player      @(rf/subscribe [:main-player])
        video-stream @(rf/subscribe [:stream])]
    [stream video-stream
     [video-container video-stream
      [player/video-player video-stream !player {}
       #(rf/dispatch [:player/initialize video-stream !player])]]]))
