(ns tubo.kiosks.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn kiosk-active?
  [& {:keys [kiosk kiosk-id service-id default-service default-kiosk path]}]
  (or (= kiosk-id kiosk)
      (and (= path "/kiosk")
           (not kiosk-id)
           (not= (js/parseInt service-id) default-service)
           (= default-kiosk kiosk))
      (and (or (= path "/") (= path "/kiosk"))
           (not kiosk-id)
           (= default-kiosk kiosk))))

(defn kiosk
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{{:keys [serviceId]} :query-params}]
      (let [{:keys [id related-streams next-page]}
            @(rf/subscribe [:kiosk])
            next-page-url (:url next-page)
            service-id (or @(rf/subscribe [:service-id]) serviceId)]
        [layout/content-container
         [layout/content-header id [items/layout-switcher !layout]]
         [items/related-streams related-streams next-page-url !layout
          #(rf/dispatch [:kiosks/fetch-paginated service-id id
                         next-page-url])]]))))
