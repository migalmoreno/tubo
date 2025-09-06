(ns tubo.services.subs
  (:require
   [re-frame.core :as rf]
   [tubo.utils :as utils]))

(rf/reg-sub
 :service-id
 (fn [db]
   (:service-id db)))

(rf/reg-sub
 :service-color
 (fn []
   (rf/subscribe [:service-id]))
 (fn [id]
   (and id (utils/get-service-color id))))

(rf/reg-sub
 :peertube/instances
 (fn [db]
   (:peertube/instances db)))

(rf/reg-sub
 :services
 (fn [db]
   (:services db)))

(rf/reg-sub
 :services/current
 (fn []
   [(rf/subscribe [:services]) (rf/subscribe [:service-id])])
 (fn [[services id]]
   (get services id)))
