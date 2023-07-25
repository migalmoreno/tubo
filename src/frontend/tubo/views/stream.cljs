(ns tubo.views.stream
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.events :as events]
   [tubo.components.items :as items]
   [tubo.components.loading :as loading]
   [tubo.components.navigation :as navigation]
   [tubo.components.comments :as comments]
   [tubo.components.player :as player]
   [tubo.util :as util]))

(defn stream
  [match]
  (let [{:keys [name url video-streams audio-streams view-count
                subscriber-count like-count dislike-count
                description uploader-avatar uploader-name
                uploader-url upload-date related-streams
                thumbnail-url show-comments-loading comments-page
                show-comments service-id] :as stream} @(rf/subscribe [:stream])
        available-streams (apply conj audio-streams video-streams)
        {:keys [content id] :as stream-format} @(rf/subscribe [:stream-format])
        page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col.items-center.justify-center.text-white.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.w-full.pb-4.relative {:class "ml:w-4/5 xl:w-3/5"}
        [navigation/back-button service-color]
        [:div.flex.justify-center.relative
         {:class "ml:h-[450px] lg:h-[600px]"}
         (when stream-format
           [player/stream-player {"sources" [{"src" content "type" "video/mp4"}
                                             {"src" content "type" "video/webm"}]
                                  "poster" thumbnail-url
                                  "controls" true}
            content])]
        [:div.px-4.ml:p-0
         [:div.flex.flex.w-full.mt-3
          (when stream-format
            [:div.relative.flex.bg-stone-800.flex-col.items-center.justify-center.z-10.mr-2.border.rounded.border-black
             [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.pl-4.pr-8.w-full
              {:on-change #(rf/dispatch [::events/change-stream-format (.. % -target -value)])
               :value id
               :style {:background "transparent"}}
              (when available-streams
                (for [[i {:keys [id format resolution averageBitrate]}] (map-indexed vector available-streams)]
                  [:option.bg-neutral-900.border-none {:value id :key i}
                   (str (or resolution "audio-only") " " format (when-not resolution (str " " averageBitrate "kbit/s")))]))]
             [:div.flex.absolute.min-h-full.top-0.right-4.items-center.justify-end
              {:style {:zIndex "-1"}}
              [:i.fa-solid.fa-caret-down]]])
          [:button.border.rounded.border-black.px-3.py-1.bg-stone-800
           {:on-click #(rf/dispatch [::events/switch-to-global-player
                                     {:uploader-name uploader-name :uploader-url uploader-url
                                      :name name :url url :stream content :service-color service-color}])}
           [:i.fa-solid.fa-headphones]]
          [:button.border.rounded.border-black.px-3.py-1.bg-stone-800.mx-2
           [:a {:href url}
            [:i.fa-solid.fa-external-link-alt]]]]
         [:div.flex.flex-col.py-1.comments
          [:div.min-w-full.py-3
           [:h1.text-2xl.font-extrabold.line-clamp-1 name]]
          [:div.flex.justify-between.py-2
           [:div.flex.items-center.flex-auto
            (when uploader-avatar
              [:div.relative.w-16.h-16
               [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
                [:img.rounded-full.object-cover.max-w-full.min-h-full {:src uploader-avatar :alt uploader-name}]]])
            [:div.mx-2
             [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]
             (when subscriber-count
               [:div.flex.my-2.items-center
                [:i.fa-solid.fa-users.text-xs]
                [:p.mx-2 (util/format-quantity subscriber-count)]])]]
           [:div.flex.flex-col.items-end
            (when view-count
              [:div
               [:i.fa-solid.fa-eye.text-xs]
               [:span.ml-2 (.toLocaleString view-count)]])
            [:div.flex
             (when like-count
               [:div.items-center
                [:i.fa-solid.fa-thumbs-up.text-xs]
                [:span.ml-2 (.toLocaleString like-count)]])
             (when dislike-count
               [:div.ml-2.items-center
                [:i.fa-solid.fa-thumbs-down.text-xs]
                [:span.ml-2 dislike-count]])]
            (when upload-date
              [:div
               [:i.fa-solid.fa-calendar.mx-2.text-xs]
               [:span
                (-> upload-date
                    js/Date.parse
                    js/Date.
                    .toDateString)]])]]
          [:div.min-w-full.py-3
           [:h1 name]
           [:div {:dangerouslySetInnerHTML {:__html description}}]]
          (when-not (empty? (:comments comments-page))
            [:div.py-6
             [:div.flex.items-center
              [:i.fa-solid.fa-comments]
              [:p.px-2.py-4 "Comments"]
              (if show-comments
                [:i.fa-solid.fa-chevron-up {:on-click #(rf/dispatch [::events/toggle-comments])
                                            :style    {:cursor "pointer"}}]
                [:i.fa-solid.fa-chevron-down {:on-click #(if (or show-comments comments-page)
                                                           (rf/dispatch [::events/toggle-comments])
                                                           (rf/dispatch [::events/get-comments url]))
                                              :style    {:cursor "pointer"}}])]
             [:div
              (if show-comments-loading
                [loading/loading-icon service-color "text-2xl"]
                (when (and show-comments comments-page)
                  [comments/comments comments-page uploader-name uploader-avatar url]))]])
          (when-not (empty? related-streams)
            [:div.py-6
             [:div.flex.items-center
              [:i.fa-solid.fa-list]
              [:h1.px-2.text-lg.bold "Related Results"]]
             [items/related-streams related-streams nil]])]]])]))
