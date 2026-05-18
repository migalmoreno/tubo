(ns tubo.player.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !players (r/atom {}))

(rf/reg-cofx
 :players
 (fn [cofx _]
   (assoc cofx :players !players)))

(rf/reg-sub
 :player-by-id
 (fn [_ _]
   !players)
 (fn [players [_ id]]
   (get players id)))

(defonce !elapsed (r/atom 0))

(rf/reg-sub
 :elapsed-time
 (fn [_ _]
   !elapsed))

(defonce !duration (r/atom nil))

(rf/reg-sub
 :player/duration
 (fn [_ _]
   !duration))

(rf/reg-cofx
 :elapsed
 (fn [cofx _]
   (assoc cofx :elapsed !elapsed)))

(defonce !paused (r/atom true))

(rf/reg-sub
 :player/paused
 (fn [_ _]
   !paused))

(defonce !buffered (r/atom 0))

(rf/reg-sub
 :player/buffered
 (fn [_ _]
   !buffered))

(rf/reg-cofx
 :buffered
 (fn [cofx _]
   (assoc cofx :buffered !buffered)))

(defonce !waiting (r/atom false))

(rf/reg-sub
 :player/waiting
 (fn [_ _]
   !waiting))

(rf/reg-cofx
 :waiting
 (fn [cofx _]
   (assoc cofx :waiting !waiting)))

(rf/reg-sub
 :player/loop
 :-> :player/loop)

(rf/reg-sub
 :player/shuffled
 :-> :player/shuffled)

(rf/reg-sub
 :player/volume
 :-> :player/volume)

(rf/reg-sub
 :player/muted
 :-> :player/muted)

(rf/reg-sub
 :bg-player/id
 :-> :bg-player/id)

(rf/reg-sub
 :bg-player
 (fn [_ _]
   [(rf/subscribe [:bg-player/id]) !players])
 (fn [[id players] _]
   (get players id)))

(rf/reg-sub
 :main-player/show
 :-> :main-player/show)

(rf/reg-sub
 :bg-player/show
 :-> :bg-player/show)
