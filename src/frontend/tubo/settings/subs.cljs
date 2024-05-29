(ns tubo.settings.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :settings
 (fn [db _]
   (:settings db)))
