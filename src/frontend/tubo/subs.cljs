(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.auth.subs]
   [tubo.bookmarks.subs]
   [tubo.channel.subs]
   [tubo.feed.subs]
   [tubo.kiosks.subs]
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

(defonce !virtuoso (atom nil))

(rf/reg-sub
 :virtuoso
 (fn []
   !virtuoso))

(defonce !breakpoint
  (let [breakpoints {:lg "(min-width: 1024px)"
                     :md "(min-width: 768px) and (max-width: 1023px)"
                     :sm "(min-width: 480px) and (max-width: 767px)"
                     :xs "(max-width: 479px)"}
        state       (r/atom
                     (when (.-matchMedia js/window)
                       (->> breakpoints
                            (filter
                             (fn [[breakpoint query]]
                               (when (.-matches (.matchMedia js/window query))
                                 breakpoint)))
                            first
                            first)))]
    (doseq [[breakpoint query] breakpoints]
      (.addEventListener (.matchMedia js/window query)
                         "change"
                         #(when (.-matches %)
                            (reset! state breakpoint))))
    state))

(rf/reg-sub
 :layout/breakpoint
 (fn [_]
   @!breakpoint))

(defonce !page-visible
  (let [state (r/atom (not (.-hidden js/document)))]
    (.addEventListener js/document
                       "visibilitychange"
                       #(reset! state (not (.-hidden js/document))))
    state))

(rf/reg-sub
 :page-visible
 (fn []
   @!page-visible))

(rf/reg-sub
 :layout/bg-overlay
 (fn [db]
   (:layout/bg-overlay db)))

(rf/reg-sub
 :layout/mobile-tooltip
 (fn [db]
   (:layout/mobile-tooltip db)))

(rf/reg-sub
 :layout/mobile-panel
 (fn [db]
   (:layout/mobile-panel db)))

(rf/reg-sub
 :layout/tooltips
 (fn [db]
   (:layout/tooltips db)))

(rf/reg-sub
 :layout/tooltip-by-id
 (fn []
   (rf/subscribe [:layout/tooltips]))
 (fn [tooltips [_ id]]
   (get tooltips id)))

(rf/reg-sub
 :layout/panels
 (fn [db]
   (:layout/panels db)))

(rf/reg-sub
 :layout/panel-by-id
 (fn []
   (rf/subscribe [:layout/panels]))
 (fn [tooltips [_ id]]
   (get tooltips id)))
