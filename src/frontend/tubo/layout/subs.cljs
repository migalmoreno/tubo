(ns tubo.layout.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !breakpoint
  (let [state (r/atom
               (if (and (.-matchMedia js/window)
                        (.-matches (.matchMedia js/window
                                                "(min-width: 768px)")))
                 :md
                 :sm))]
    (.addEventListener (.matchMedia js/window "(max-width: 768px)")
                       "change"
                       #(reset! state (if (.-matches %) :sm :md)))
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
