(ns tubo.views.kiosk
  (:require
   [re-frame.core :as rf]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn kiosk
  [{{:keys [serviceId kioskId]} :query-params}]
  (let [{:keys [id url related-streams next-page]} @(rf/subscribe [:kiosk])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/kiosk-pagination serviceId id next-page-url]))
    [layout/content-container
     [layout/content-header id]
     [items/related-streams related-streams next-page-url]]))
