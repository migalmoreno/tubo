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
  [icon on-click & {:keys [disabled? show-on-mobile? extra-classes]}]
  [:button.outline-none.focus:ring-transparent.px-2.pt-1
   {:class    (let [classes (apply conj (when disabled? ["opacity-50" "cursor-auto"])
                                   (when-not show-on-mobile? ["hidden" "lg:block"]))]
                (apply str (if (> (count extra-classes) 1) (interpose " " (conj classes extra-classes)) (interpose " " classes))))
    :on-click on-click}
   icon])

(defn loop-button
  [loop-playback service-color show-on-mobile?]
  [button
   [:div.relative.flex.items-center
    [:i.fa-solid.fa-repeat
     {:style {:color (when loop-playback service-color)}}]
    (when (= loop-playback :stream)
      [:div.absolute.font-bold
       {:style {:color     (when loop-playback service-color)
                :font-size "5.5px"
                :right     "6px"
                :top       "-4px"}}
       "1"])]
   #(rf/dispatch [::events/toggle-loop-playback])
   :extra-classes "text-sm"
   :show-on-mobile? show-on-mobile?])

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
         [:input.rounded-lg.w-24.cursor-pointer.focus:outline-none.absolute
          {:class    "rotate-[270deg]"
           :type     "range"
           :on-input #(rf/dispatch [::events/change-volume-level (.. % -target -value) player])
           :style    {:accentColor service-color
                      :left "-32.5px"
                      :bottom "63px"}
           :max      100
           :value    volume-level}])])))
