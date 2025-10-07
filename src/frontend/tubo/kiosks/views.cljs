(ns tubo.kiosks.views
  (:require
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
  [{{:keys [serviceId]} :query-params}]
  (let [{:keys [id name next-page] :as kiosk} @(rf/subscribe [:kiosk])
        service-id                            (or @(rf/subscribe [:service-id])
                                                  serviceId)]
    [layout/content-container
     [layout/content-header name]
     [items/related-items (:related-items kiosk) next-page
      (:items-layout @(rf/subscribe [:settings]))
      #(rf/dispatch [:kiosks/fetch-paginated service-id id next-page])]]))
