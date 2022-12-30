(ns tau.views.stream
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.events :as events]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]
   [tau.components.comments :as comments]
   [tau.util :as util]))

(defn stream
  [match]
  (let [{:keys [name url video-streams audio-streams view-count
                subscriber-count like-count dislike-count
                description uploader-avatar uploader-name
                uploader-url upload-date related-streams
                thumbnail-url show-comments-loading comments-page
                show-comments service-id] :as stream} @(rf/subscribe [:stream])
        stream-type (-> (if (empty? video-streams) audio-streams video-streams)
                        last
                        :content)
        page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col.items-center.justify-center.text-white.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.w-full.pb-4 {:class "ml:w-4/5 xl:w-3/5"}
        [navigation/back-button service-color]
        [:div.flex.justify-center.relative
         {:style {:background (str "center / cover no-repeat url('" thumbnail-url"')")}
          :class "ml:h-[450px] lg:h-[600px]"}
         [:video.bottom-0.object-cover.max-h-full.min-w-full
          {:src stream-type :controls true}]]
        [:div.px-4.ml:p-0
         [:div.flex.flex.w-full.mt-3.justify-center
          [:button.border.rounded.border-black.px-3.py-1.bg-stone-800
           {:on-click #(rf/dispatch [::events/switch-to-global-player
                                     {:uploader-name uploader-name :uploader-url uploader-url
                                      :name name :url url :stream stream-type :service-color service-color}])}
           [:i.fa-solid.fa-headphones]]
          [:a {:href url}
           [:button.border.rounded.border-black.px-3.py-1.bg-stone-800.mx-2
            [:i.fa-solid.fa-external-link-alt]]]]
         [:div.flex.flex-col.py-1.comments
          [:div.min-w-full.py-3
           [:h1.text-2xl.font-extrabold.line-clamp-1 name]]
          [:div.flex.justify-between.py-2
           [:div.flex.items-center.flex-auto
            (when uploader-avatar
              [:div.relative.w-16.h-16
               [:a {:href (rfe/href :tau.routes/channel nil {:url uploader-url}) :title uploader-name}
                [:img.rounded-full.object-cover.max-w-full.min-h-full {:src uploader-avatar :alt uploader-name}]]])
            [:div.mx-2
             [:a {:href (rfe/href :tau.routes/channel nil {:url uploader-url})} uploader-name]
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
          [:div.py-6
           [:div.flex.items-center
            [:i.fa-solid.fa-comments]
            [:p.px-2.py-4 "Comments"]
            (if show-comments
              [:i.fa-solid.fa-chevron-up {:on-click #(rf/dispatch [::events/toggle-comments])
                                          :style {:cursor "pointer"}}]
              [:i.fa-solid.fa-chevron-down {:on-click #(if (or show-comments comments-page)
                                                         (rf/dispatch [::events/toggle-comments])
                                                         (rf/dispatch [::events/get-comments url]))
                                            :style {:cursor "pointer"}}])]
           [:div
            (if show-comments-loading
              [loading/loading-icon service-color "text-2xl"]
              (when (and show-comments comments-page)
                [comments/comments comments-page uploader-name uploader-avatar url]))]]
          (when-not (empty? related-streams)
            [:div.py-3
             [:div.flex.items-center
              [:i.fa-solid.fa-list]
              [:h1.px-2.text-lg.bold "Related Results"]]
             [items/related-streams related-streams nil]])]]])]))
