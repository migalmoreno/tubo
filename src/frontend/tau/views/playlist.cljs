(ns tau.views.playlist
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]
   [tau.events :as events]))

(defn playlist
  [{{:keys [url]} :query-params}]
  (let [{:keys [id name playlist-type thumbnail-url banner-url
                uploader-name uploader-url uploader-avatar stream-count
                next-page related-streams]} @(rf/subscribe [:playlist])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        page-scroll @(rf/subscribe [:page-scroll])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/playlist-pagination url next-page-url]))
    [:div.flex.flex-col.items-center.px-5.pt-4.flex-auto
     (if page-loading?
       [loading/page-loading-icon service-color]
       [:div.flex.flex-col.flex-auto.w-full {:class "lg:w-4/5"}
        [navigation/back-button service-color]
        (when banner-url
          [:div
           [:img {:src banner-url}]])
        [:div.flex.items-center.justify-center.my-4.mx-2
         [:div.flex.flex-col.justify-center.items-center
          [:h1.text-2xl.font-bold name]
          [:div.flex.items-center.pt-4
           [:span.mr-2 "By"]
           [:div.flex.items-center.py-3.box-border.h-12
            [:div.w-12
             [:a {:href (rfe/href :tau.routes/channel nil {:url uploader-url}) :title uploader-name}
              [:img.rounded-full.object-cover.min-h-full.min-w-full {:src uploader-avatar :alt uploader-name}]]]]]
          [:p.pt-4 (str stream-count " streams")]]]
        [items/related-streams related-streams next-page-url]])]))
