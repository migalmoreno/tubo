(ns tau.routes
  (:require
   [reitit.frontend :as ref]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [re-frame.core :as rf]
   [tau.views.home :as home]
   [tau.views.search :as search]
   [tau.views.stream :as stream]))

(def routes
  (ref/router
   [["/" {:view home/home-page
          :name ::home}]
    ["/search" {:view search/search
                :name ::search
                :controllers [{:parameters {:query [:q :serviceId]}
                               :start (fn [parameters]
                                        (rf/dispatch [:get-search-results
                                                      {:id (-> parameters :query :serviceId)
                                                       :query (-> parameters :query :q)}]))}]}]
    ["/stream" {:view stream/stream
                :name ::stream
                :controllers [{:parameters {:query [:url]}
                               :start (fn [parameters]
                                        (rf/dispatch [:get-stream (-> parameters :query :url)]))}]}]]))

(defn on-navigate
  [new-match]
  (let [old-match (rf/subscribe [:current-match])]
    (when new-match
      (let [controllers (rfc/apply-controllers (:controllers @old-match) new-match)
            match (assoc new-match :controllers controllers)]
        (rf/dispatch [:navigated match])))))

(defn start-routes!
  []
  (rfe/start! routes on-navigate {:use-fragment false}))
