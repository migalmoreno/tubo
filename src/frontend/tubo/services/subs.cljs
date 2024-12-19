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
 :service-name
 (fn []
   (rf/subscribe [:service-id]))
 (fn [id]
   (and id (utils/get-service-name id))))

(rf/reg-sub
 :services
 (fn [db]
   (:services db)))
