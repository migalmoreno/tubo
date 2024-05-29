(ns tubo.notifications.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :notifications
 (fn [db _]
   (:notifications db)))
