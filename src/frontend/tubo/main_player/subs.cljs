(ns tubo.main-player.subs
  (:require
   [re-frame.core :as rf]))

(defonce !player (atom nil))

(rf/reg-sub
 :main-player/ready
 (fn [db _]
   (:main-player/ready db)))

(rf/reg-sub
 :main-player/show
 (fn [db _]
   (:main-player/show db)))

(rf/reg-sub
 :main-player
 (fn [_ _]
   !player))
