(ns tubo.views
  (:require
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
  (let [current-match @(rf/subscribe [:navigation/current-match])
        dark-theme?   @(rf/subscribe [:dark-theme])]
    [:div
     {:class    (when dark-theme? :dark)
      :on-click #(rf/dispatch [:layout/destroy-tooltips-on-click-out
                               (.. % -target)])}
     [:div.font-nunito-sans.min-h-screen.h-full.relative.flex.flex-col.dark:text-white.bg-neutral-100.dark:bg-neutral-950.z-10
      [layout/background-overlay]
      [layout/mobile-tooltip]
      [modals/modals-container]
      [navigation/mobile-menu current-match]
      [navigation/navbar current-match]
      [notifications/notifications-panel]
      [:div.flex.flex-auto
       [navigation/sidebar current-match]
       [:div.flex.flex-col.flex-auto.justify-between.relative.max-w-full
        (when-let [view (-> current-match
                            :data
                            :view)]
          [view current-match])
        [queue/queue]
        [bg-player/player]
        [main-player/player]]]]]))
