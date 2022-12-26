(ns tau.views.kiosk
  (:require
   [re-frame.core :as rf]))

(defn kiosk
  [match]
  (let [{:keys [id url related-streams]} @(rf/subscribe [:kiosk])]
    [:div
     [:h1 id]]))
