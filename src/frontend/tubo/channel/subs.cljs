(ns tubo.channel.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :channel
 (fn [db _]
   (:channel db)))
