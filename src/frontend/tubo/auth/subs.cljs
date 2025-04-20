(ns tubo.auth.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :auth/user
 (fn [db]
   (:auth/user db)))
