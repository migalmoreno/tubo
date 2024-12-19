(ns tubo.modals.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :modals
 (fn [db]
   (:modals db)))
