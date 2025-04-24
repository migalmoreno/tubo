(ns tubo.bookmarks.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :user/bookmarks
 (fn [db]
   (:user/bookmarks db)))

(rf/reg-sub
 :bookmarks
 (fn [db]
   (if (:auth/user db)
     (:user/bookmarks db)
     (:bookmarks db))))

(rf/reg-sub
 :bookmarks/bookmarked
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ id]]
   (some #(= (:id %) id) (rest bookmarks))))

(rf/reg-sub
 :bookmarks/favorited
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ url]]
   (some #(= (:url %) url)
         (-> bookmarks
             first
             :items))))

(rf/reg-sub
 :bookmarks/playlisted
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ url playlist-id]]
   (some #(= (:url %) url)
         (->> bookmarks
              (filter #(= (:id %) playlist-id))
              first
              :items))))
