(ns tubo.bookmarks.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :bookmarks
 (fn [db _]
   (:bookmarks db)))
