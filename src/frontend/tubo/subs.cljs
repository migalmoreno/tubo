(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.bg-player.subs]
   [tubo.bookmarks.subs]
   [tubo.channel.subs]
   [tubo.kiosks.subs]
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

(defonce !is-window-visible
  (let [a (r/atom true)]
    (.addEventListener js/window "focus" #(reset! a true))
    (.addEventListener js/window "blur" #(reset! a false))
    a))

(defonce !scroll-distance
  (let [a (r/atom 0)
        compute-scroll-distance
        #(when (> (.-scrollY js/window) 0)
           (reset! a (+ (.-scrollY js/window) (.-innerHeight js/window))))]
    (.addEventListener js/window "scroll" compute-scroll-distance)
    (.addEventListener js/window "touchmove" compute-scroll-distance)
    a))

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
 :is-window-visible
 (fn [_ _]
   @!is-window-visible))

(rf/reg-sub
 :scrolled-to-bottom
 (fn [_ _]
   (> (+ @!scroll-distance 35) (.-scrollHeight js/document.body))))

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
