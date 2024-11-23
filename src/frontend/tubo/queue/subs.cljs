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
 :queue-pos
 (fn [db _]
   (:queue-pos db)))

(rf/reg-sub
 :show-queue
 (fn [db _]
   (:show-queue db)))

(rf/reg-sub
 :queue-stream
 (fn [_]
   [(rf/subscribe [:queue]) (rf/subscribe [:queue-pos])])
 (fn [[queue pos] _]
   (and (not-empty queue) (< pos (count queue)) (nth queue pos))))
