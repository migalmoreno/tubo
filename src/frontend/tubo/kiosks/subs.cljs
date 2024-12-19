(ns tubo.kiosks.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :kiosks
 (fn [db]
   (:kiosks db)))

(rf/reg-sub
 :kiosk
 (fn [db]
   (:kiosk db)))
