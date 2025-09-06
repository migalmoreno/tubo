(ns tubo.router
  (:require
   [re-frame.core :as rf]
   [reitit.core :as r]
   [reitit.frontend :as ref]
   [reitit.frontend.easy :as rfe]
   [tubo.about.views :as about]
   [tubo.auth.views :as auth]
   [tubo.bookmarks.views :as bookmarks]
   [tubo.channel.views :as channel]
   [tubo.feed.views :as feed]
   [tubo.kiosks.views :as kiosk]
   [tubo.playlist.views :as playlist]
   [tubo.routes :as routes]
   [tubo.search.views :as search]
   [tubo.settings.views :as settings]
   [tubo.stream.views :as stream]
   [tubo.subscriptions.views :as subscriptions]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (case data
      :web/homepage      {:view        kiosk/kiosk
                          :name        :homepage
                          :controllers [{:start #(rf/dispatch
                                                  [:fetch-homepage])}]}
      :web/register      {:view auth/register
                          :name :register-page}
      :web/login         {:view auth/login
                          :name :login-page}
      :web/search        {:view        search/search
                          :name        :search-page
                          :controllers [{:parameters {:query [:q :serviceId
                                                              :filter]}
                                         :start      (fn [{{:keys [serviceId q
                                                                   filter]}
                                                           :query}]
                                                       (rf/dispatch
                                                        [:search/fetch-page
                                                         serviceId
                                                         q
                                                         filter]))
                                         :stop       #(rf/dispatch
                                                       [:search/leave-page])}]}
      :web/stream        {:view stream/stream
                          :name :stream-page
                          :controllers
                          [{:parameters {:query [:url]}
                            :start (fn [{{:keys [url]} :query}]
                                     (rf/dispatch
                                      [:stream/fetch-page
                                       url])
                                     (rf/dispatch
                                      [:navigation/show-sidebar
                                       false]))
                            :stop
                            #(rf/dispatch
                              [:navigation/change-show-sidebar-value
                               (if (and
                                    (.-matchMedia js/window)
                                    (.-matches
                                     (.matchMedia
                                      js/window
                                      "(max-width: 1280px)")))
                                 :minimized
                                 :expanded)])}]}
      :web/channel       {:view        channel/channel
                          :name        :channel-page
                          :controllers [{:parameters {:query [:url]}
                                         :start      (fn [{{:keys [url]}
                                                           :query}]
                                                       (rf/dispatch
                                                        [:channel/fetch-page
                                                         url]))}]}
      :web/playlist      {:view        playlist/playlist-page
                          :name        :playlist-page
                          :controllers [{:parameters {:query [:url]}
                                         :start      (fn [{{:keys [url]}
                                                           :query}]
                                                       (rf/dispatch
                                                        [:playlist/fetch-page
                                                         url]))}]}
      :web/kiosk         {:view        kiosk/kiosk
                          :name        :kiosk-page
                          :controllers [{:parameters {:query [:kioskId
                                                              :serviceId]}
                                         :start      (fn [{{:keys [serviceId
                                                                   kioskId]}
                                                           :query}]
                                                       (rf/dispatch
                                                        [:kiosks/fetch-page
                                                         serviceId
                                                         kioskId]))}]}
      :web/settings      {:view        settings/settings
                          :name        :settings-page
                          :controllers [{:start #(rf/dispatch
                                                  [:settings/fetch-page])}]}
      :web/bookmark      {:view        bookmarks/bookmark
                          :name        :bookmark-page
                          :controllers [{:parameters {:query [:id]}
                                         :start      (fn [{{:keys [id]} :query}]
                                                       (rf/dispatch
                                                        [:bookmark/fetch-page
                                                         id]))}]}
      :web/bookmarks     {:view        bookmarks/bookmarks
                          :name        :bookmarks-page
                          :controllers [{:start #(rf/dispatch
                                                  [:bookmarks/fetch-page])}]}
      :web/subscriptions {:view        subscriptions/subscriptions-page
                          :name        :subscriptions-page
                          :controllers [{:start
                                         #(rf/dispatch
                                           [:subscriptions/fetch-page])}]}
      :web/feed          {:view        feed/feed-page
                          :name        :feed-page
                          :controllers [{:start
                                         #(rf/dispatch
                                           [:feed/fetch-page])}]}
      :web/about         {:view about/about
                          :name :about-page}
      :web/privacy       {:view about/privacy-policy
                          :name :privacy-page}
      nil)
    (r/expand data opts)))

(def router
  (ref/router routes/routes {:expand expand-routes}))

(defn on-navigate
  [new-match]
  (when new-match
    (rf/dispatch [:navigation/navigated new-match])))

(defn start-router!
  []
  (rfe/start! router on-navigate {:use-fragment false}))
