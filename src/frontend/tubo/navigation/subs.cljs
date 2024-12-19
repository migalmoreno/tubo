(ns tubo.navigation.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :navigation/show-mobile-menu
 (fn [db]
   (:navigation/show-mobile-menu db)))

(rf/reg-sub
 :navigation/current-match
 (fn [db]
   (:navigation/current-match db)))
