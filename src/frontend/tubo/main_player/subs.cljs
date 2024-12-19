(ns tubo.main-player.subs
  (:require
   [re-frame.core :as rf]))

(defonce !player (atom nil))

(rf/reg-sub
 :main-player/ready
 (fn [db]
   (:main-player/ready db)))

(rf/reg-sub
 :main-player/show
 (fn [db]
   (:main-player/show db)))

(rf/reg-sub
 :main-player
 (fn []
   !player))
