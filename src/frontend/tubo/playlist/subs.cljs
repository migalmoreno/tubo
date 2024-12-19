(ns tubo.playlist.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :playlist
 (fn [db]
   (:playlist db)))
