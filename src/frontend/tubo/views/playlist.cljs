(ns tubo.views.playlist
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.loading :as loading]
   [tubo.components.navigation :as navigation]
   [tubo.events :as events]))

(defn playlist
  [{{:keys [url]} :query-params}]
  (let [{:keys [id name playlist-type thumbnail-url banner-url
                uploader-name uploader-url uploader-avatar stream-count
                next-page related-streams]} @(rf/subscribe [:playlist])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/playlist-pagination url next-page-url]))
    [:div.flex.flex-col.items-center.px-5.pt-4.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full {:class "ml:w-4/5"}
        [navigation/back-button service-color]
        (when banner-url
          [:div
           [:img {:src banner-url}]])
        [:div.flex.flex-col.justify-center.my-4.mx-2
         [:div.flex.justify-between.items-center.mb-4
          [:div
           [:h1.text-2xl.font-bold.line-clamp-1 {:title name} name]]
          [:div.flex-auto.whitespace-nowrap.ml-4
           [:button
            {:on-click #(rf/dispatch [::events/enqueue-related-streams related-streams service-color])}
            [:i.fa-solid.fa-headphones]
            [:span.ml-4.text-neutral-600.dark:text-neutral-300 "Background"]]]]
         [:div.flex.items-center.justify-between
          [:div.flex.items-center.my-4.mr-2
           [:div.flex.items-center.py-3.pr-3.box-border.h-12
            [:div.w-12
             [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
              [:img.rounded-full.object-cover.min-h-full.min-w-full {:src uploader-avatar :alt uploader-name}]]]]
           [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
            uploader-name]]
          [:span.ml-2.whitespace-nowrap (str stream-count " streams")]]
         [:div.flex.flex.w-full.my-4.justify-center
          ]]
        [items/related-streams related-streams next-page-url]])]))
