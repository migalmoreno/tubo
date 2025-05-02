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
   (map #(if (:thumbnail %)
           %
           (assoc % :thumbnail (:thumbnail (first (:items %)))))
        (get db (if (:auth/user db) :user/bookmarks :bookmarks)))))

(rf/reg-sub
 :bookmarks/get-by-id
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ id]]
   (first (filter #(= (or (:playlist-id %)
                          (:id %))
                      id)
                  bookmarks))))

(rf/reg-sub
 :bookmarks/bookmarked
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ id]]
   (some #(= (or (:playlist-id %) (:id %)) id) bookmarks)))

(rf/reg-sub
 :bookmarks/playlisted
 (fn [db]
   (rf/subscribe (if (:auth/user db) [:user/bookmarks] [:bookmarks])))
 (fn [bookmarks [_ url playlist-id]]
   (some #(= (:url %) url)
         (->> bookmarks
              (filter #(= (or (:playlist-id %) (:id %)) playlist-id))
              first
              :items))))
