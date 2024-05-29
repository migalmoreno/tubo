(ns tubo.player.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !player (atom nil))
(defonce !elapsed-time (r/atom 0))

(rf/reg-sub
 :player
 (fn [db _]
   !player))

(rf/reg-sub
 :player-ready
 (fn [db _]
   (:player-ready db)))

(rf/reg-sub
 :show-background-player
 (fn [db _]
   (:show-background-player db)))

(rf/reg-sub
 :show-background-player-loading
 (fn [db _]
   (:show-background-player-loading db)))

(rf/reg-sub
 :loop-playback
 (fn [db _]
   (:loop-playback db)))

(rf/reg-sub
 :paused
 (fn [db _]
   (:paused db)))

(rf/reg-sub
 :volume-level
 (fn [db _]
   (:volume-level db)))

(rf/reg-sub
 :muted
 (fn [db _]
   (:muted db)))

(rf/reg-sub
 :elapsed-time
 (fn [db _]
   !elapsed-time))
