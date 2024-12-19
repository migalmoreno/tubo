(ns tubo.search.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :search/results
 (fn [db]
   (:search/results db)))

(rf/reg-sub
 :search/query
 (fn [db]
   (:search/query db)))

(rf/reg-sub
 :search/show-form
 (fn [db]
   (:search/show-form db)))
