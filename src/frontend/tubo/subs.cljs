(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.bg-player.subs]
   [tubo.bookmarks.subs]
   [tubo.channel.subs]
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
   [tubo.stream.subs]))

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
 (fn [_]
   (rf/subscribe [:settings]))
 (fn [{:keys [theme]} _]
   (or (and (= theme "auto") (= @!auto-theme "dark")) (= theme "dark"))))

(rf/reg-sub
 :page-scroll
 (fn [db _]
   (:page-scroll db)))

(rf/reg-sub
 :show-page-loading
 (fn [db _]
   (:show-page-loading db)))

(rf/reg-sub
 :show-pagination-loading
 (fn [db _]
   (:show-pagination-loading db)))
