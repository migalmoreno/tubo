(ns tau.routes
  (:require
   [reitit.frontend :as ref]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [re-frame.core :as rf]
   [tau.events :as events]
   [tau.views.channel :as channel]
   [tau.views.home :as home]
   [tau.views.kiosk :as kiosk]
   [tau.views.playlist :as playlist]
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
                                        (rf/dispatch [::events/change-service-id
                                                      (js/parseInt (-> parameters :query :serviceId))])
                                        (rf/dispatch [::events/get-search-results
                                                      {:service-id (-> parameters :query :serviceId)
                                                       :query (-> parameters :query :q)}]))}]}]
    ["/stream" {:view stream/stream
                :name ::stream
                :controllers [{:parameters {:query [:url]}
                               :start (fn [parameters]
                                        (rf/dispatch [::events/get-stream (-> parameters :query :url)]))}]}]
    ["/channel" {:view channel/channel
                 :name ::channel
                 :controllers [{:parameters {:query [:url]}
                                :start (fn [parameters]
                                         (rf/dispatch [::events/get-channel (-> parameters :query :url)]))}]}]
    ["/playlist" {:view playlist/playlist
                  :name ::playlist
                  :controllers [{:parameters {:query [:url]}
                                 :start (fn [parameters]
                                          (rf/dispatch [::events/get-playlist (-> parameters :query :url)]))}]}]
    ["/kiosk" {:view kiosk/kiosk
               :name ::kiosk
               :controllers [{:parameters {:query [:kioskId :serviceId]}
                              :start (fn [parameters]
                                       (rf/dispatch [::events/get-kiosk
                                                     {:service-id (-> parameters :query :serviceId)
                                                      :kiosk-id (-> parameters :query :kioskId)}]))}]}]]))

(defn on-navigate
  [new-match]
  (let [old-match (rf/subscribe [:current-match])]
    (when new-match
      (let [controllers (rfc/apply-controllers (:controllers @old-match) new-match)
            match (assoc new-match :controllers controllers)]
        (rf/dispatch [::events/navigated match])))))

(defn start-routes!
  []
  (rfe/start! routes on-navigate {:use-fragment false}))
