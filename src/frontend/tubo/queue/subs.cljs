(ns tubo.queue.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :queue
 (fn [db]
   (:queue db)))

(rf/reg-sub
 :queue/unshuffled
 (fn [db]
   (:queue/unshuffled db)))

(rf/reg-sub
 :queue/position
 (fn [db]
   (:queue/position db)))

(rf/reg-sub
 :queue/show
 (fn [db]
   (:queue/show db)))

(rf/reg-sub
 :queue/current
 (fn []
   [(rf/subscribe [:queue]) (rf/subscribe [:queue/position])])
 (fn [[queue pos]]
   (and (not-empty queue) (< pos (count queue)) (nth queue pos))))
