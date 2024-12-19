(ns tubo.layout.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :layout/bg-overlay
 (fn [db]
   (:layout/bg-overlay db)))

(rf/reg-sub
 :layout/mobile-tooltip
 (fn [db]
   (:layout/mobile-tooltip db)))

(rf/reg-sub
 :layout/tooltips
 (fn [db]
   (:layout/tooltips db)))

(rf/reg-sub
 :layout/tooltip-by-id
 (fn []
   (rf/subscribe [:layout/tooltips]))
 (fn [tooltips [_ id]]
   (get tooltips id)))
