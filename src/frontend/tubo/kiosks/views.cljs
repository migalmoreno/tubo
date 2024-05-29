(ns tubo.kiosks.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]))

(defn kiosk-active?
  [& {:keys [kiosk kiosk-id service-id default-service default-kiosk path]}]
  (or (and (= kiosk-id kiosk))
      (and (= path "/kiosk")
           (not kiosk-id)
           (not= (js/parseInt service-id)
                 (:service-id default-service))
           (= default-kiosk kiosk))
      (and (or (= path "/") (= path "/kiosk"))
           (not kiosk-id)
           (= (:default-kiosk default-service) kiosk))))

(defn kiosks-menu
  [& {:keys [kiosks service-id] :as kiosk-args}]
  [:ul.flex.items-center.px-4.text-white
   (for [kiosk kiosks]
     [:li.px-3 {:key kiosk}
      [:a {:href  (rfe/href :kiosk-page nil {:serviceId service-id
                                             :kioskId   kiosk})
           :class (when (kiosk-active? (assoc kiosk-args :kiosk kiosk))
                    :font-bold)}
       kiosk]])])

(defn kiosk
  [{{:keys [serviceId kioskId]} :query-params}]
  (let [{:keys [id url related-streams
                next-page]} @(rf/subscribe [:kiosk])
        next-page-url       (:url next-page)
        service-color       @(rf/subscribe [:service-color])
        service-id          (or @(rf/subscribe [:service-id]) serviceId)
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [:kiosks/fetch-paginated service-id id next-page-url]))
    [layout/content-container
     [layout/content-header id]
     [items/related-streams related-streams next-page-url]]))
