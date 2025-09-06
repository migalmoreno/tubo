(ns tubo.feed.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :feed
 (fn [db]
   (get db (if (:auth/user db) :user/feed :feed))))

(rf/reg-sub
 :user/feed
 :-> :user/feed)

(rf/reg-sub
 :feed-last-updated
 (fn [db]
   (get db (if (:auth/user db) :user/feed-last-updated :feed-last-updated))))

(rf/reg-sub
 :user/feed-last-updated
 :-> :user/feed-last-updated)
