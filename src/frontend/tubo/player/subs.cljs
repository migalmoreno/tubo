(ns tubo.player.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !elapsed-time (r/atom 0))

(rf/reg-sub
 :player/loop
 (fn [db _]
   (:player/loop db)))

(rf/reg-sub
 :player/shuffled
 (fn [db _]
   (:player/shuffled db)))

(rf/reg-sub
 :player/paused
 (fn [db _]
   (:player/paused db)))

(rf/reg-sub
 :player/volume
 (fn [db _]
   (:player/volume db)))

(rf/reg-sub
 :player/muted
 (fn [db _]
   (:player/muted db)))

(rf/reg-sub
 :elapsed-time
 (fn [_ _]
   !elapsed-time))
