(ns tubo.subscriptions.views
  (:require
   [re-frame.core :as rf]
   [tubo.ui :as ui]))

(defn subscriptions-page
  []
  (let [subscriptions @(rf/subscribe [:subscriptions])]
    [ui/content-container
     [ui/content-header "Subscriptions"
      [:div.flex.flex-auto
       [ui/popover
        [{:label    "Clear all"
          :icon     [:i.fa-solid.fa-trash]
          :on-click #(rf/dispatch [:subscriptions/clear])}]]]]
     [ui/related-items (map #(assoc % :info-type "CHANNEL") subscriptions)
      nil "grid"]]))
