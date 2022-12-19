(ns tau.views.player
  (:require
   [re-frame.core :as rf]))

(defn global-player
  []
  (let [global-stream @(rf/subscribe [:global-stream])
        show-global-player? @(rf/subscribe [:show-global-player])]
    [:div
     [:audio {:src global-stream
              :class (when-not show-global-player? "hidden")}]]))
