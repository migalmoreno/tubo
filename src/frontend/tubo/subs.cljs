(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.auth.subs]
   [tubo.bg-player.subs]
   [tubo.bookmarks.subs]
   [tubo.channel.subs]
   [tubo.feed.subs]
   [tubo.kiosks.subs]
   [tubo.layout.subs]
   [tubo.main-player.subs]
   [tubo.modals.subs]
   [tubo.navigation.subs]
   [tubo.notifications.subs]
   [tubo.player.subs]
   [tubo.playlist.subs]
   [tubo.queue.subs]
   [tubo.search.subs]
   [tubo.services.subs]
   [tubo.settings.subs]
   [tubo.stream.subs]
   [tubo.subscriptions.subs]))

(defonce !auto-theme
  (let [theme (r/atom (when (and (.-matchMedia js/window)
                                 (.-matches
                                  (.matchMedia js/window
                                               "(prefers-color-scheme: dark)")))
                        "dark"))]
    (.addEventListener (.matchMedia js/window "(prefers-color-scheme: dark)")
                       "change"
                       #(reset! theme (if (.-matches %) "dark" "light")))
    theme))

(rf/reg-sub
 :dark-theme
 (fn []
   (rf/subscribe [:settings]))
 (fn [{:keys [theme]}]
   (or (and (= theme "auto") (= @!auto-theme "dark")) (= theme "dark"))))

(rf/reg-sub
 :show-pagination-loading
 (fn [db]
   (:show-pagination-loading db)))

(defonce !top-loading-bar (atom nil))

(rf/reg-sub
 :top-loading-bar
 (fn []
   !top-loading-bar))

(rf/reg-sub
