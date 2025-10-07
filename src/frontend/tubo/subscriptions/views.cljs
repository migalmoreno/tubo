(ns tubo.subscriptions.views
  (:require
   [tubo.layout.views :as layout]
   [tubo.items.views :as items]
   [re-frame.core :as rf]))

(defn subscriptions-page
  []
  (let [subscriptions @(rf/subscribe [:subscriptions])]
    [layout/content-container
     [layout/content-header "Subscriptions"
      [:div.flex.flex-auto
       [layout/popover
        [{:label    "Clear all"
          :icon     [:i.fa-solid.fa-trash]
          :on-click #(rf/dispatch [:subscriptions/clear])}]]]]
     [items/related-items (map #(assoc % :info-type "CHANNEL") subscriptions)
      nil "grid"]]))
