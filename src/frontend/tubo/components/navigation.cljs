(ns tubo.components.navigation
  (:require
   [re-frame.core :as rf]
   [tubo.events :as events]))

(defn back-button [service-color]
  [:div.flex.items-center
   [:button.py-4.px-2
    {:on-click #(rf/dispatch [::events/history-back])}
    [:i.fa-solid.fa-chevron-left
     {:style {:color service-color}}]
    [:span " Back"]]])
