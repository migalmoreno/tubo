(ns tubo.search.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :search/results
 (fn [db _]
   (:search/results db)))

(rf/reg-sub
 :search/query
 (fn [db _]
   (:search/query db)))

(rf/reg-sub
 :search/show-form
 (fn [db _]
   (:search/show-form db)))
