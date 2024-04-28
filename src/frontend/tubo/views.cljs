(ns tubo.views
  (:require
   [re-frame.core :as rf]
   [tubo.components.audio-player :as player]
   [tubo.components.navigation :as navigation]
   [tubo.components.notification :as notification]
   [tubo.components.play-queue :as queue]
   [tubo.events :as events]))

(defn app
  []
  (let [current-match   @(rf/subscribe [:current-match])
        auto-theme      @(rf/subscribe [:auto-theme])
        {:keys [theme]} @(rf/subscribe [:settings])]
    [:div {:class (when (or (and (= theme "auto") (= auto-theme :dark)) (= theme "dark")) "dark")}
     [:div.min-h-screen.flex.flex-col.h-full.dark:text-white.bg-neutral-100.dark:bg-neutral-900.relative.font-nunito-sans
      [navigation/navbar current-match]
      [notification/notifications-panel]
      [:div.flex.flex-col.flex-auto.justify-between.relative
       (when-let [view (-> current-match :data :view)]
         [view current-match])
       [queue/queue]
       [player/player]]]]))
