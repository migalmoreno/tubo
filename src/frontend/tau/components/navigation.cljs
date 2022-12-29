(ns tau.components.navigation
  (:require
   [re-frame.core :as rf]
   [tau.events :as events]))

(defn back-button [service-color]
  [:div.flex {:class "w-4/5"}
   [:button.p-2
    {:on-click #(rf/dispatch [::events/history-back])}
    [:i.fa-solid.fa-chevron-left
     {:style {:color service-color}}]
    [:span " Back"]]])
