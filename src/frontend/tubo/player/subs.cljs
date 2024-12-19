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

(rf/reg-sub
 :player/paused
 (fn [db]
   (:player/paused db)))

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
