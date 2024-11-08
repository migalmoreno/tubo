(ns tubo.views
  (:require
   [re-frame.core :as rf]
   [tubo.components.navigation :as navigation]
   [tubo.notifications.views :as notifications]
   [tubo.player.views :as player]
   [tubo.queue.views :as queue]))

(defn app
  []
  (let [current-match @(rf/subscribe [:current-match])
        dark-theme?   @(rf/subscribe [:dark-theme])]
    [:div {:class (when dark-theme? :dark)}
     [:div.font-nunito-sans.min-h-screen.h-full.relative.flex.flex-col.dark:text-white.bg-neutral-100.dark:bg-neutral-900
      [navigation/navbar current-match]
      [notifications/notifications-panel]
      [:div.flex.flex-col.flex-auto.justify-between.relative
       (when-let [view (-> current-match
                           :data
                           :view)]
         [view current-match])
       [queue/queue]
       [player/main-player]
       [player/background-player]]]]))
