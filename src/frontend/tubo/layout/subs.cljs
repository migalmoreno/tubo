(ns tubo.layout.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !breakpoint
  (let [breakpoints {:lg "(min-width: 1025px)"
                     :md "(min-width: 769px) and (max-width: 1024px)"
                     :sm "(max-width: 768px)"}
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

(rf/reg-sub
 :layout/bg-overlay
 (fn [db]
   (:layout/bg-overlay db)))

(rf/reg-sub
 :layout/mobile-tooltip
 (fn [db]
   (:layout/mobile-tooltip db)))

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
