(ns tau.views.kiosk
  (:require
   [re-frame.core :as rf]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]
   [tau.events :as events]))

(defn kiosk
  [{{:keys [serviceId kioskId]} :query-params}]
  (let [{:keys [id url related-streams next-page]} @(rf/subscribe [:kiosk])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        page-scroll @(rf/subscribe [:page-scroll])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/kiosk-pagination serviceId id next-page-url]))
    [:div.flex.flex-col.items-center.px-5.py-2.flex-auto
     (if page-loading?
       [loading/page-loading-icon service-color]
       [:div
        [:div.flex.justify-center.items-center.my-4.mx-2
         [:div.m-4
          [:h1.text-2xl id]]]
        [:div.flex.justify-center.items-center.align-center
         [:div.flex.justify-start.flex-wrap
          (for [[i item] (map-indexed vector related-streams)]
            (case (:type item)
              "stream" [items/stream-item (assoc item :key i)]
              "channel" [items/channel-item (assoc item :key i)]
              "playlist" [items/playlist-item (assoc item :key i)]))]]
        (when-not (empty? next-page-url)
           [loading/items-pagination-loading-icon service-color pagination-loading?])])]))
