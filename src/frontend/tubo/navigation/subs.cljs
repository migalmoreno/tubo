(ns tubo.navigation.subs
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defonce !sidebar-match-media-state
  (let [state (r/atom
               (if (and (.-matchMedia js/window)
                        (.-matches (.matchMedia js/window
                                                "(max-width: 1280px)")))
                 :minimized
                 :expanded))]
    (.addEventListener (.matchMedia js/window "(max-width: 1280px)")
                       "change"
                       #(do
                          (reset! state (if (.-matches %) :minimized :expanded))
                          (rf/dispatch
                           [:navigation/show-sidebar
                            (if (.-matches %) :minimized :expanded)])))
    state))

(rf/reg-sub
 :navigation/show-mobile-menu
 (fn [db]
   (:navigation/show-mobile-menu db)))

(rf/reg-sub
 :navigation/sidebar-match-media-state
 (fn [_]
   @!sidebar-match-media-state))

(rf/reg-sub
 :navigation/show-sidebar
 (fn [db]
   (:navigation/show-sidebar db)))

(rf/reg-sub
 :navigation/current-match
 (fn [db]
   (:navigation/current-match db)))

(rf/reg-sub
 :navigation/sidebar-minimized
 (fn []
   [(rf/subscribe [:navigation/show-sidebar])
    (rf/subscribe [:navigation/sidebar-match-media-state])])
 (fn [[show-sidebar? sidebar-state]]
   (cond
     (and (or (= show-sidebar? :minimized) (nil? show-sidebar?))
          (= sidebar-state :minimized))
     true
     (and (= show-sidebar? :minimized) (= sidebar-state :expanded)) true
     (and (nil? show-sidebar?) (= sidebar-state :expanded)) false)))

(rf/reg-sub
 :navigation/sidebar-shown
 (fn []
   [(rf/subscribe [:navigation/show-sidebar])
    (rf/subscribe [:navigation/sidebar-match-media-state])])
 (fn [[show-sidebar? sidebar-state]]
   (and (not (false? show-sidebar?)) (not (false? sidebar-state)))))
