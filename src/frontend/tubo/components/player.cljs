(ns tubo.components.player
  (:require
   [reagent.core :as r]
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

(defn button
  [icon on-click-cb & {:keys [disabled? show-on-mobile? extra-styles]}]
  [:button.outline-none.focus:ring-transparent.px-2.pt-1
   {:class    (let [styles (apply conj (when disabled? ["opacity-50" "cursor-auto"])
                                  (when-not show-on-mobile? ["hidden" "lg:block"]))]
                (apply str (if (> (count extra-styles) 1) (interpose " " (conj styles extra-styles)) (interpose " " styles))))
    :on-click on-click-cb}
   icon])

(defn volume-slider [player volume-level muted? service-color]
  (let [show-slider? (r/atom nil)]
    (fn [player volume-level muted? service-color]
      [:div.relative.flex.items-center
       {:on-mouse-over #(reset! show-slider? true)
        :on-mouse-out #(reset! show-slider? false)}
       [button
        (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
        #(rf/dispatch [::events/toggle-mute player])]
       (when @show-slider?
         [:input.rounded-lg.cursor-pointer.focus:outline-none.absolute
          {:class    "rotate-[270deg]"
           :type     "range"
           :on-input #(rf/dispatch [::events/change-volume-level (.. % -target -value) player])
           :style    {:accentColor service-color :left "-48px" :bottom "80px" :display (if @show-slider? "block" "none")}
           :max      100
           :value    volume-level}])])))
