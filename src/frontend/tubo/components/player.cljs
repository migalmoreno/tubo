(ns tubo.components.player
  (:require
   [re-frame.core :as rf]
   [tubo.events :as events]))

(defn time-slider [player elapsed-time service-color]
  [:input.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.w-full
   {:type      "range"
    :on-input  #(reset! elapsed-time (.. % -target -value))
    :on-change #(and @player (> (.-readyState @player) 0)
                     (set! (.-currentTime @player) @elapsed-time))
    :style     {:accentColor service-color}
    :max       (if (and @player (> (.-readyState @player) 0))
                 (.floor js/Math (.-duration @player))
                 100)
    :value     @elapsed-time}])

(defn volume-slider [player volume-level service-color]
  [:input.w-20.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.range-sm.mx-2
   {:type     "range"
    :on-input #(rf/dispatch [::events/change-volume-level (.. % -target -value) @player])
    :style    {:accentColor service-color}
    :max      100
    :value    volume-level}])

(defn button
  [icon on-click-cb & {:keys [disabled? show-on-mobile? extra-styles]}]
  [:button.outline-none.focus:ring-transparent.mx-2
   {:class    (let [styles (apply conj (when disabled? ["opacity-50" "cursor-auto"])
                                  (when-not show-on-mobile? ["hidden" "lg:block"]))]
                (apply str (if (> (count extra-styles) 1) (interpose " " (conj styles extra-styles)) (interpose " " styles))))
    :on-click on-click-cb}
   icon])
