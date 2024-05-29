(ns tubo.stream.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :stream
 (fn [db _]
   (:stream db)))
