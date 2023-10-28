(ns tubo.views.kiosk
  (:require
   [re-frame.core :as rf]
   [tubo.components.items :as items]
   [tubo.components.loading :as loading]
   [tubo.components.navigation :as navigation]
   [tubo.events :as events]))

(defn kiosk
  [{{:keys [serviceId kioskId]} :query-params}]
  (let [{:keys [id url related-streams next-page]} @(rf/subscribe [:kiosk])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/kiosk-pagination serviceId id next-page-url]))
    [:div.flex.flex-col.items-center.px-5.py-2.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full {:class "lg:w-4/5 xl:w-3/5"}
        [:div.flex.justify-center.items-center.my-4.mx-2
         [:div.m-4
          [:h1.text-2xl id]]]
        [items/related-streams related-streams next-page-url]])]))
