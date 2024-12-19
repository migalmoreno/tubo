(ns tubo.bookmarks.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :bookmarks
 (fn [db]
   (:bookmarks db)))

(rf/reg-sub
 :bookmarks/bookmarked
 (fn []
   (rf/subscribe [:bookmarks]))
 (fn [bookmarks [_ id]]
   (some #(= (:id %) id) (rest bookmarks))))

(rf/reg-sub
 :bookmarks/favorited
 (fn []
   (rf/subscribe [:bookmarks]))
 (fn [bookmarks [_ url]]
   (some #(= (:url %) url)
         (-> bookmarks
             first
             :items))))

(rf/reg-sub
 :bookmarks/playlisted
 (fn []
   (rf/subscribe [:bookmarks]))
 (fn [bookmarks [_ url playlist-id]]
   (some #(= (:url %) url)
         (->> bookmarks
              (filter #(= (:id %) playlist-id))
              first
              :items))))
