(ns tubo.views.stream
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.events :as events]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.comments :as comments]
   [tubo.components.video-player :as player]
   [tubo.util :as util]))

(defn stream
  [match]
  (let [{:keys [name url video-streams audio-streams view-count
                subscriber-count like-count dislike-count
                description uploader-avatar uploader-name
                uploader-url upload-date related-streams
                thumbnail-url show-comments-loading comments-page
                show-comments show-related show-description service-id]
         :as stream} @(rf/subscribe [:stream])
        {show-comments? :show-comments show-related? :show-related
         show-description? :show-description} @(rf/subscribe [:settings])
        available-streams (apply conj audio-streams video-streams)
        {:keys [content id] :as stream-format} @(rf/subscribe [:stream-format])
        page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])
        bookmarks @(rf/subscribe [:bookmarks])]
    [layout/content-container
     [:div.flex.justify-center.relative
      {:class "h-[300px] lg:h-[450px] lg:h-[600px]"}
      (when stream-format
        [player/player {"sources"    [{"src" content "type" "video/mp4"}
                                      {"src" content "type" "video/webm"}]
                        "poster"     thumbnail-url
                        "controls"   true
                        "responsive" true
                        "fill"       true}
         content])]
     [:div.px-4.lg:p-0.overflow-x-hidden
      [:div.flex.flex.w-full.my-4.justify-center
       [:button.sm:px-2.py-1.text-sm.sm:text-base.text-neutral-600.dark:text-neutral-300
        {:on-click #(rf/dispatch [::events/switch-to-audio-player stream service-color])}
        [:i.fa-solid.fa-headphones]
        [:span.mx-3 "Background"]]
       (if (some #(= (:url %) url) bookmarks)
         [:button.sm:px-2.py-1.text-sm.sm:text-base.text-neutral-600.dark:text-neutral-300
          {:on-click #(rf/dispatch [::events/remove-from-bookmarks stream])}
          [:i.fa-solid.fa-bookmark]
          [:span.mx-3 "Bookmarked"]]
         [:button.sm:px-2.py-1.text-sm.sm:text-base.text-neutral-600.dark:text-neutral-300
          {:on-click #(rf/dispatch [::events/add-to-bookmarks stream])}
          [:i.fa-regular.fa-bookmark]
          [:span.mx-3 "Bookmark"]])
       [:button.sm:px-2.py-1.text-sm.sm:text-base.text-neutral-600.dark:text-neutral-300
        [:a.block.sm:inline-block {:href url}
         [:i.fa-solid.fa-external-link-alt]
         [:span.mx-3 "Original"]]]
       (when stream-format
         [:div.relative.flex.flex-col.items-center.justify-center.text-neutral-600.dark:text-neutral-300
          [:select.border-none.focus:ring-transparent.dark:bg-blend-color-dodge.pr-8.w-full.text-ellipsis.text-sm.sm:text-base
           {:on-change #(rf/dispatch [::events/change-stream-format (.. % -target -value)])
            :value     id
            :style     {:background "transparent"}}
           (when available-streams
             (for [[i {:keys [id format resolution averageBitrate]}] (map-indexed vector available-streams)]
               [:option.dark:bg-neutral-900.border-none {:value id :key i}
                (str (or resolution "audio-only") " " format (when-not resolution (str " " averageBitrate "kbit/s")))]))]
          [:div.flex.absolute.min-h-full.top-0.right-4.items-center.justify-end
           [:i.fa-solid.fa-caret-down]]])]
      [:div.flex.flex-col
       [:div.min-w-full.pb-3
        [:h1.text-2xl.font-extrabold.line-clamp-1 name]]
       [:div.flex.justify-between.py-2
        [:div.flex.items-center
         [layout/uploader-avatar uploader-avatar uploader-name
          (rfe/href :tubo.routes/channel nil {:url uploader-url})]
         [:div.mx-3
          [:a.line-clamp-1 {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]
          (when subscriber-count
            [:div.flex.my-2.items-center
             [:i.fa-solid.fa-users.text-xs]
             [:p.mx-2 (util/format-quantity subscriber-count)]])]]
        [:div.flex.flex-col.items-end.flex-auto
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
            [:i.fa-solid.fa-calendar.mx-2]
            [:span
             (-> upload-date
                 js/Date.parse
                 js/Date.
                 .toDateString)]])]]
       (when (and show-description? description)
         [:div.py-3.flex.flex-wrap.min-w-full
          [:div {:dangerouslySetInnerHTML {:__html description}
                 :class                   (when (not show-description) "line-clamp-2")}]
          [:div.flex.justify-center.font-bold.min-w-full.pt-4.cursor-pointer
           [layout/secondary-button
            (if (not show-description) "Show More" "Show Less")
            #(rf/dispatch [::events/toggle-stream-layout :show-description])]]])
       (when (and comments-page (not (empty? (:comments comments-page))) show-comments?)
         [:div
          [:div.flex.items-center
           [:i.fa-solid.fa-comments.w-6]
           [:h2.mx-4.text-lg "Comments"]
           (if show-comments
             [:i.fa-solid.fa-chevron-up.cursor-pointer.text-sm
              {:on-click #(rf/dispatch [::events/toggle-stream-layout :show-comments])}]
             [:i.fa-solid.fa-chevron-down.cursor-pointer.text-sm.ml-2
              {:on-click #(if (or show-comments comments-page)
                            (rf/dispatch [::events/toggle-stream-layout :show-comments])
                            (rf/dispatch [::events/get-comments url]))}])]
          (if show-comments-loading
            [layout/loading-icon service-color "text-2xl"]
            (when (and show-comments comments-page)
              [comments/comments comments-page uploader-name uploader-avatar url]))])
       (when (and show-related? (not (empty? related-streams)))
         [:div.pt-2
          [:div.flex.justify-between
           [:div.flex.items-center.text-sm.sm:text-base
            [:i.fa-solid.fa-list.w-6]
            [:h2.mx-4.text-lg "Suggested"]
            [:i.fa-solid.fa-chevron-up.cursor-pointer.text-sm
             {:class    (if (not show-related) "fa-chevron-up" "fa-chevron-down")
              :on-click #(rf/dispatch [::events/toggle-stream-layout :show-related])}]]
           [layout/primary-button "Enqueue"
            #(rf/dispatch [::events/enqueue-related-streams related-streams service-color])
            "fa-solid fa-headphones"]]
          (when (not show-related)
            [items/related-streams related-streams nil])])]]]))
