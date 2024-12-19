(ns tubo.bg-player.subs
  (:require
   [re-frame.core :as rf]))

(defonce !player (atom nil))

(rf/reg-sub
 :bg-player/ready
 (fn [db]
   (:bg-player/ready db)))

(rf/reg-sub
 :bg-player/show
 (fn [db]
   (:bg-player/show db)))

(rf/reg-sub
 :bg-player/loading
 (fn [db]
   (:bg-player/loading db)))

(rf/reg-sub
 :bg-player
 (fn []
   !player))
