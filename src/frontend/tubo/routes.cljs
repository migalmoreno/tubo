(ns tubo.routes
  (:require
   [reitit.frontend :as ref]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [tubo.channel.views :as channel]
   [tubo.kiosks.views :as kiosk]
   [tubo.playlist.views :as playlist]
   [tubo.bookmarks.views :as bookmarks]
   [tubo.search.views :as search]
   [tubo.settings.views :as settings]
   [tubo.stream.views :as stream]))

(def router
  (ref/router
   [["/" {:view        kiosk/kiosk
          :name        :homepage
          :controllers [{:start #(rf/dispatch [:fetch-homepage])}]}]
    ["/search" {:view        search/search
                :name        :search-page
                :controllers [{:parameters {:query [:q :serviceId]}
                               :start      (fn [{{:keys [serviceId q]} :query}]
                                             (rf/dispatch [:search/fetch-page serviceId q]))
                               :stop       #(rf/dispatch [:search/show-form false])}]}]
    ["/stream" {:view        stream/stream
                :name        :stream-page
                :controllers [{:parameters {:query [:url]}
                               :start      (fn [{{:keys [url]} :query}]
                                             (rf/dispatch [:stream/fetch-page url]))}]}]
    ["/channel" {:view        channel/channel
                 :name        :channel-page
                 :controllers [{:parameters {:query [:url]}
                                :start      (fn [{{:keys [url]} :query}]
                                              (rf/dispatch [:channel/fetch-page url]))}]}]
    ["/playlist" {:view        playlist/playlist
                  :name        :playlist-page
                  :controllers [{:parameters {:query [:url]}
                                 :start      (fn [{{:keys [url]} :query}]
                                               (rf/dispatch [:playlist/fetch-page url]))}]}]
    ["/kiosk" {:view        kiosk/kiosk
               :name        :kiosk-page
               :controllers [{:parameters {:query [:kioskId :serviceId]}
                              :start      (fn [{{:keys [serviceId kioskId]} :query}]
                                            (rf/dispatch [:kiosks/fetch-page serviceId kioskId]))}]}]
    ["/settings" {:view        settings/settings
                  :name        :settings-page
                  :controllers [{:start #(rf/dispatch [:settings/fetch-page])}]}]
    ["/bookmark" {:view        bookmarks/bookmark
                  :name        :bookmark-page
                  :controllers [{:parameters {:query [:id]}
                                 :start      (fn [{{:keys [id]} :query}]
                                               (rf/dispatch [:bookmark/fetch-page id]))}]}]
    ["/bookmarks" {:view        bookmarks/bookmarks
                   :name        :bookmarks-page
                   :controllers [{:start #(rf/dispatch [:bookmarks/fetch-page])}]}]]))

(defn on-navigate
  [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match])))

(defn start-routes!
  []
  (rfe/start! router on-navigate {:use-fragment false}))
