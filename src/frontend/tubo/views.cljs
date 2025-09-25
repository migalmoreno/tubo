(ns tubo.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   ["react-top-loading-bar$default" :as LoadingBar]
   [re-frame.core :as rf]
   [tubo.bg-player.views :as bg-player]
   [tubo.layout.views :as layout]
   [tubo.main-player.views :as main-player]
   [tubo.modals.views :as modals]
   [tubo.navigation.views :as navigation]
   [tubo.notifications.views :as notifications]
   [tubo.queue.views :as queue]))

(defn app
  []
  (let [current-match    @(rf/subscribe [:navigation/current-match])
        dark-theme?      @(rf/subscribe [:dark-theme])
        !top-loading-bar @(rf/subscribe [:top-loading-bar])]
    [:div
     {:class    (when dark-theme? :dark)
      :on-click #(rf/dispatch [:layout/destroy-tooltips-on-click-out
                               (.. % -target)])}
     [:div.font-roboto.min-h-screen.h-full.relative.flex.flex-col.dark:text-white.bg-neutral-100.dark:bg-neutral-950.z-10
      [layout/background-overlay]
      [layout/mobile-tooltip]
      [modals/modals-container]
      [navigation/mobile-menu current-match]
      [navigation/navbar current-match]
      [notifications/notifications-panel]
      [:div.flex.flex-auto
       [:> LoadingBar
        {:color @(rf/subscribe [:service-color])
         :ref   #(reset! !top-loading-bar %)}]
       [navigation/sidebar current-match]
       [:div.flex.flex-col.flex-auto.justify-between.relative.max-w-full
        [:> AnimatePresence
         {:mode "wait" :onExitComplete #(rf/dispatch [:scroll-to-top])}
         (if-let [view (get-in current-match [:data :view])]
           ^{:key (get-in current-match [:data :name])}
           [:> (.-div motion)
            {:class      ["flex" "flex-auto"]
             :exit       {:opacity 0}
             :transition {:duration 0.5 :ease "easeOut"}}
            [view current-match]]
           [layout/not-found-page])]
        [queue/queue]
        [bg-player/player]
        [main-player/player]]]]]))
