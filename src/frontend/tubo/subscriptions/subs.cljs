(ns tubo.subscriptions.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :subscriptions
 (fn [db]
   (get db (if (:auth/user db) :user/subscriptions :subscriptions))))

(rf/reg-sub
 :user/subscriptions
 :-> :user/subscriptions)

(rf/reg-sub
 :subscriptions/subscribed
 (fn []
   (rf/subscribe [:subscriptions]))
 (fn [subscriptions [_ url]]
   (some #(= (:url %) url) subscriptions)))
