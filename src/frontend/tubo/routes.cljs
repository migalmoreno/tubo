(ns tubo.routes
  (:require
   [reitit.frontend :as ref]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [tubo.events :as events]
   [tubo.views.channel :as channel]
   [tubo.views.kiosk :as kiosk]
   [tubo.views.playlist :as playlist]
   [tubo.views.playlists :as playlists]
   [tubo.views.search :as search]
   [tubo.views.settings :as settings]
   [tubo.views.stream :as stream]))

(def router
  (ref/router
   [["/" {:view kiosk/kiosk
          :name ::home
          :controllers [{:start #(rf/dispatch [::events/get-homepage])}]}]
    ["/search" {:view search/search
                :name ::search
                :controllers [{:parameters {:query [:q :serviceId]}
                               :start (fn [{{:keys [serviceId q]} :query}]
                                        (rf/dispatch [::events/get-search-page serviceId q]))}]}]
    ["/stream" {:view stream/stream
                :name ::stream
                :controllers [{:parameters {:query [:url]}
                               :start (fn [{{:keys [url]} :query}]
                                        (rf/dispatch [::events/get-stream-page url]))}]}]
    ["/channel" {:view channel/channel
                 :name ::channel
                 :controllers [{:parameters {:query [:url]}
                                :start (fn [{{:keys [url]} :query}]
                                         (rf/dispatch [::events/get-channel-page url]))}]}]
    ["/playlist" {:view playlist/playlist
                  :name ::playlist
                  :controllers [{:parameters {:query [:url]}
                                 :start (fn [{{:keys [url]} :query}]
                                          (rf/dispatch [::events/get-playlist-page url]))}]}]
    ["/kiosk" {:view kiosk/kiosk
               :name ::kiosk
               :controllers [{:parameters {:query [:kioskId :serviceId]}
                              :start (fn [{{:keys [serviceId kioskId]} :query}]
                                       (rf/dispatch [::events/get-kiosk-page serviceId kioskId]))}]}]
    ["/settings" {:view settings/settings-page
                  :name ::settings
                  :controllers [{:start #(rf/dispatch [::events/get-settings-page])}]}]
    ["/playlists" {:view playlists/playlists-page
                   :name ::playlists
                   :controllers [{:start #(rf/dispatch [::events/get-playlists-page])}]}]]))

(defn on-navigate
  [new-match]
  (when new-match
    (rf/dispatch [::events/navigated new-match])))

(defn start-routes!
  []
  (rfe/start! router on-navigate {:use-fragment false}))
