(ns tubo.search.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :search-results
 (fn [db _]
   (:search-results db)))

(rf/reg-sub
 :search-query
 (fn [db _]
   (:search-query db)))

(rf/reg-sub
 :show-search-form
 (fn [db _]
   (:show-search-form db)))
