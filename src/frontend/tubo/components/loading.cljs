(ns tubo.components.loading)

(defn loading-icon
  [service-color & styles]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fas.fa-circle-notch.fa-spin
    {:class (apply str (if (> (count styles) 1) (interpose " " styles) styles))
     :style {:color service-color}}]])
