(ns tau.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :search-results
 (fn [db _]
   (:search-results db)))

(rf/reg-sub
 :stream
 (fn [db _]
   (:stream db)))

(rf/reg-sub
 :global-search
 (fn [db _]
   (:global-search db)))

(rf/reg-sub
 :service-id
 (fn [db _]
   (:service-id db)))

(rf/reg-sub
 :services
 (fn [db _]
   (:services db)))

(rf/reg-sub
 :current-match
 (fn [db _]
   (:current-match db)))

(rf/reg-sub
 :global-stream
 (fn [db _]
   (:global-stream db)))

(rf/reg-sub
 :show-global-player
 (fn [db _]
   (:show-global-player db)))
