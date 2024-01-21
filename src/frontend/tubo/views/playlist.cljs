(ns tubo.views.playlist
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.loading :as loading]
   [tubo.events :as events]))

(defn playlist
  [{{:keys [url]} :query-params}]
  (let [{:keys [id name playlist-type thumbnail-url banner-url
                uploader-name uploader-url uploader-avatar stream-count
                next-page related-streams]} @(rf/subscribe [:playlist])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/playlist-pagination url next-page-url]))
    [layout/content-container
     [:div.flex.flex-col.justify-center.my-4.mx-2
      [:div.flex.justify-between.items-center.mb-4
       [:h1.text-2xl.font-bold.line-clamp-1.pr-2 {:title name} name]
       [layout/primary-button "Enqueue"
        #(rf/dispatch [::events/enqueue-related-streams related-streams service-color]) "fa-solid fa-headphones"]]
      [:div.flex.items-center.justify-between
       [:div.flex.items-center.my-4.mr-2
        [layout/uploader-avatar uploader-avatar uploader-name uploader-url]
        [:div
         [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
          uploader-name]]]
       [:span.ml-2.whitespace-nowrap (str stream-count " streams")]]]
     [items/related-streams related-streams next-page-url]]))
