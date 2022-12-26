(ns tau.views.stream
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.events :as events]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]))

(defn stream
  [match]
  (let [{:keys [name url video-streams audio-streams view-count
                subscriber-count like-count dislike-count
                description upload-avatar upload-author
                upload-url upload-date related-streams
                thumbnail-url] :as stream} @(rf/subscribe [:stream])
        stream-type (-> (if (empty? video-streams) audio-streams video-streams)
                        last
                        :content)
        page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col.p-5.items-center.justify-center.text-white.flex-auto
     (if page-loading?
       [loading/page-loading-icon service-color]
       [:div {:class "w-4/5"}
        [navigation/back-button]
        [:div.flex.justify-center.relative.my-2
         {:style {:background (str "center / cover no-repeat url('" thumbnail-url"')")
                  :height "450px"}}
         [:video.min-h-full.absolute.bottom-0.object-cover {:src stream-type :controls true :width "100%"}]]
        [:div.flex.text-white.flex.w-full.my-1
         [:button.border.rounded.border-black.p-2.bg-stone-800
          {:on-click #(rf/dispatch [::events/switch-to-global-player stream])}
          [:i.fa-solid.fa-headphones]]
         [:a {:href (:url stream)}
          [:button.border.rounded.border-black.p-2.bg-stone-800.mx-2
           [:i.fa-solid.fa-external-link-alt]]]]
        [:div.flex.flex-col.py-1
         [:div.min-w-full.py-3
          [:h1.text-xl.font-extrabold name]]
         [:div.flex.justify-between.py-2
          [:div.flex.items-center.flex-auto
           (when upload-avatar
             [:div
              [:img.rounded-full {:src upload-avatar :alt upload-author}]])
           [:div.mx-2
            [:a {:href (rfe/href :tau.routes/channel nil {:url upload-url})} upload-author]
            (when subscriber-count
              [:div.flex.my-2
               [:i.fa-solid.fa-users]
               [:p.mx-2 (.toLocaleString subscriber-count)]])]]
          [:div
           (when view-count
             [:p
              [:i.fa-solid.fa-eye]
              [:span.mx-2 (.toLocaleString view-count)]])
           [:div
            (when like-count
              [:p
               [:i.fa-solid.fa-thumbs-up]
               [:span.mx-2 like-count]])
            (when dislike-count
              [:p
               [:i.fa-solid.fa-thumbs-down]
               [:span.mx-2 dislike-count]])]
           (when upload-date
             [:p (-> upload-date
                     js/Date.parse
                     js/Date.
                     .toDateString)])]]
         [:div.min-w-full.py-3
          [:h1 name]
          [:p description]]
         [:div.py-3
          [:h1.text-lg.bold "Related Results"]
          [:div.flex.justify-center.align-center.flex-wrap
           (for [[i item] (map-indexed vector related-streams)]
             (cond
               (:duration item) [items/stream-item i item]
               (:subscriber-count item) [items/channel-item i item]
               (:stream-count item) [items/playlist-item i item]))]]]])]))
