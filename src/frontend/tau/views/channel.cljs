(ns tau.views.channel
  (:require
   [re-frame.core :as rf]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]
   [tau.events :as events]))

(defn channel
  [{{:keys [url]} :query-params}]
  (let [{:keys [banner avatar name description subscriber-count
                related-streams next-page]} @(rf/subscribe [:channel])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-scroll @(rf/subscribe [:page-scroll])
        page-loading? @(rf/subscribe [:show-page-loading])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/scroll-channel-pagination url next-page-url]))
    [:div.flex.flex-col.items-center.px-5.py-2.text-white.flex-auto
     (if page-loading?
       [loading/page-loading-icon service-color]
       [:div {:class "w-4/5"}
        [navigation/back-button]
        [:div [:img {:src banner}]]
        [:div.flex.items-center.my-4.mx-2
         [:div
          [:img.rounded-full {:src avatar}]]
         [:div.m-4
          [:h1.text-xl name]
          [:div.flex.my-2.items-center
           [:i.fa-solid.fa-users]
           [:span.mx-2 subscriber-count]]]]
        [:div.my-2
         [:p description]]
        [:div.flex.justify-center.align-center.flex-wrap.my-2
         (for [[i result] (map-indexed vector related-streams)]
           [items/stream-item i result])]
        (when-not (empty? next-page-url)
           [loading/pagination-loading-icon service-color pagination-loading?])])]))
