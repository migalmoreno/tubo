(ns tubo.stream.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :stream
 (fn [db]
   (:stream db)))

(defonce !player (atom nil))

(rf/reg-sub
 :stream-player
 (fn []
   !player))
