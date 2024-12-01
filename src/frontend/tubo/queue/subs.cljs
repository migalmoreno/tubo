(ns tubo.queue.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :queue
 (fn [db _]
   (:queue db)))

(rf/reg-sub
 :queue/unshuffled
 (fn [db _]
   (:queue/unshuffled db)))

(rf/reg-sub
 :queue/position
 (fn [db _]
   (:queue/position db)))

(rf/reg-sub
 :queue/show
 (fn [db _]
   (:queue/show db)))

(rf/reg-sub
 :queue/current
 (fn [_]
   [(rf/subscribe [:queue]) (rf/subscribe [:queue/position])])
 (fn [[queue pos] _]
   (and (not-empty queue) (< pos (count queue)) (nth queue pos))))
