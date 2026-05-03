(ns tubo.player.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !elapsed-time (r/atom 0))

(rf/reg-sub
 :player/loop
 (fn [db]
   (:player/loop db)))

(rf/reg-sub
 :player/shuffled
 (fn [db]
   (:player/shuffled db)))

(defonce !paused (r/atom true))

(rf/reg-sub
 :player/paused
 (fn [] !paused))

(rf/reg-sub
 :player/volume
 (fn [db]
   (:player/volume db)))

(rf/reg-sub
 :player/muted
 (fn [db]
   (:player/muted db)))

(rf/reg-sub
 :elapsed-time
 (fn []
   !elapsed-time))

(defonce !main-player (atom nil))

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
   !main-player))

(defonce !bg-player (atom nil))

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
   !bg-player))

(defonce !buffered (r/atom 0))

(rf/reg-sub
 :bg-player/buffered
 (fn []
   !buffered))
