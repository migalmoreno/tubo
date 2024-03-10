(ns tubo.views.playlist
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn playlist
  [{{:keys [url]} :query-params}]
  (let [!menu-active? (r/atom nil)]
    (fn []
      (let [{:keys [id name playlist-type thumbnail-url banner-url
                    uploader-name uploader-url uploader-avatar
                    stream-count next-page
                    related-streams]} @(rf/subscribe [:playlist])
            next-page-url             (:url next-page)
            scrolled-to-bottom?       @(rf/subscribe [:scrolled-to-bottom])]
        (when scrolled-to-bottom?
          (rf/dispatch [::events/playlist-pagination url next-page-url]))
        [layout/content-container
         [:div.flex.flex-col.justify-center
          [layout/content-header name
           (when related-streams
             [layout/popover-menu !menu-active?
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [::events/enqueue-related-streams related-streams])}]])]
          [:div.flex.items-center.justify-between.my-4.gap-x-4
           [:div.flex.items-center
            [layout/uploader-avatar uploader-avatar uploader-name uploader-url]
            [:a.line-clamp-1.ml-2
             {:href  (rfe/href :tubo.routes/channel nil {:url uploader-url})
              :title uploader-name}
             uploader-name]]
           [:span.text-sm.whitespace-nowrap (str stream-count " streams")]]]
         [items/related-streams related-streams next-page-url]]))))
