(ns tubo.search.subs
  (:require
   [re-frame.core :as rf]))

(defonce !search-input (atom nil))

(rf/reg-sub
 :search/results
 (fn [db]
   (:search/results db)))

(rf/reg-sub
 :search/query
 (fn [db]
   (:search/query db)))

(rf/reg-sub
 :search/service-suggestions
 (fn []
   [(rf/subscribe [:search/suggestions]) (rf/subscribe [:service-id])])
 (fn [[query id]]
   (get query id)))

(rf/reg-sub
 :search/show-form
 (fn [db]
   (:search/show-form db)))

(rf/reg-sub
 :search/filter
 (fn [db]
   (:search/filter db)))

(rf/reg-sub
 :search/show-suggestions
 (fn [db]
   (:search/show-suggestions db)))

(rf/reg-sub
 :search/suggestions
 (fn [db]
   (:search/suggestions db)))

(rf/reg-sub
 :search-input
 (fn []
   !search-input))
