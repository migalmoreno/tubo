(ns tubo.views
  (:require
   [re-frame.core :as rf]
   [tubo.components.audio-player :as player]
   [tubo.components.navigation :as navigation]
   [tubo.components.play-queue :as queue]
   [tubo.events :as events]))

(defn app
  []
  (let [current-match   @(rf/subscribe [:current-match])
        {:keys [theme]} @(rf/subscribe [:settings])]
    [:div {:class (when (= theme "dark") "dark")}
     [:div.min-h-screen.flex.flex-col.h-full.dark:text-white.dark:bg-neutral-900.relative
      [navigation/navbar current-match]
      [:div.flex.flex-col.flex-auto.justify-between.relative.font-nunito
       (when-let [view (-> current-match :data :view)]
         [view current-match])
       [queue/queue]
       [player/player]]]]))
