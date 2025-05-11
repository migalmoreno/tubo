(ns tubo.bg-player.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

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
 :bg-player/waiting
 (fn [db]
   (:bg-player/waiting db)))

(rf/reg-sub
 :bg-player
 (fn []
   !player))

(defonce !buffered (r/atom 0))

(rf/reg-sub
 :bg-player/buffered
 (fn []
   !buffered))
