(ns tubo.navigation.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :navigation/show-mobile-menu
 (fn [db _]
   (:navigation/show-mobile-menu db)))

(rf/reg-sub
 :navigation/current-match
 (fn [db _]
   (:navigation/current-match db)))
