(ns tubo.components.player
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.events :as events]))

(defonce base-slider-classes
  ["h-2" "cursor-pointer" "appearance-none" "bg-neutral-300" "dark:bg-neutral-600"
   "rounded-full" "overflow-hidden" "focus:outline-none"
   "[&::-webkit-slider-thumb]:appearance-none"
   "[&::-webkit-slider-thumb]:border-0"
   "[&::-webkit-slider-thumb]:rounded-full"
   "[&::-webkit-slider-thumb]:h-2"
   "[&::-webkit-slider-thumb]:w-2"
   "[&::-webkit-slider-thumb]:shadow-[-405px_0_0_400px]"
   "[&::-moz-range-thumb]:border-0"
   "[&::-moz-range-thumb]:rounded-full"
   "[&::-moz-range-thumb]:h-2"
   "[&::-moz-range-thumb]:w-2"
   "[&::-moz-range-thumb]:shadow-[-405px_0_0_400px]"])

(defn get-slider-shadow-classes
  [service-color]
  (case service-color
    "#cc0000" ["[&::-webkit-slider-thumb]:shadow-[#cc0000]" "[&::-moz-range-thumb]:shadow-[#cc0000]"]
    "#ff7700" ["[&::-webkit-slider-thumb]:shadow-[#ff7700]" "[&::-moz-range-thumb]:shadow-[#ff7700]"]
    "#333333" ["[&::-webkit-slider-thumb]:shadow-[#333333]" "[&::-moz-range-thumb]:shadow-[#333333]"]
    "#F2690D" ["[&::-webkit-slider-thumb]:shadow-[#F2690D]" "[&::-moz-range-thumb]:shadow-[#F2690D]"]
    "#629aa9" ["[&::-webkit-slider-thumb]:shadow-[#629aa9]" "[&::-moz-range-thumb]:shadow-[#629aa9]"]
    ["[&::-webkit-slider-thumb]:shadow-neutral-300" "[&::-moz-range-thumb]:shadow-neutral-300"]))

(defn get-slider-bg-classes
  [service-color]
  (case service-color
    "#cc0000" ["[&::-webkit-slider-thumb]:bg-[#cc0000]" "[&::-moz-range-thumb]:bg-[#cc0000]"]
    "#ff7700" ["[&::-webkit-slider-thumb]:bg-[#ff7700]" "[&::-moz-range-thumb]:bg-[#ff7700]"]
    "#333333" ["[&::-webkit-slider-thumb]:bg-[#333333]" "[&::-moz-range-thumb]:bg-[#333333]"]
    "#F2690D" ["[&::-webkit-slider-thumb]:bg-[#F2690D]" "[&::-moz-range-thumb]:bg-[#F2690D]"]
    "#629aa9" ["[&::-webkit-slider-thumb]:bg-[#629aa9]" "[&::-moz-range-thumb]:bg-[#629aa9]"]
    ["[&::-webkit-slider-thumb]:bg-neutral-300" "[&::-moz-range-thumb]:bg-neutral-300"]))

(defn time-slider [player elapsed-time service-color]
  (let [styles `(~@base-slider-classes
                 ~@(get-slider-bg-classes service-color)
                 ~@(get-slider-shadow-classes service-color))]
    [:input.w-full
     {:class     (clojure.string/join " " styles)
      :type      "range"
      :on-input  #(reset! elapsed-time (.. % -target -value))
      :on-change #(and @player (> (.-readyState @player) 0)
                       (set! (.-currentTime @player) @elapsed-time))
      :max       (if (and @player (> (.-readyState @player) 0))
                   (.floor js/Math (.-duration @player))
                   100)
      :value     @elapsed-time}]))

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
      [:div.absolute.w-full.h-full.flex.justify-center.items-center.font-bold
       {:class "text-[6px]"
        :style {:color     (when loop-playback service-color)}}
       "1"])]
   #(rf/dispatch [::events/toggle-loop-playback])
   :extra-classes "text-sm"
   :show-on-mobile? show-on-mobile?])

(defn volume-slider [player volume-level muted? service-color]
  (let [show-slider? (r/atom nil)]
    (fn [player volume-level muted? service-color]
      (let [styles `("rotate-[270deg]"
                     ~@base-slider-classes
                     ~@(get-slider-bg-classes service-color)
                     ~@(get-slider-shadow-classes service-color))]
        [:div.relative.flex.flex-col.justify-center.items-center
         {:on-mouse-over #(reset! show-slider? true)
          :on-mouse-out  #(reset! show-slider? false)}
         [button
          (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
          #(rf/dispatch [::events/toggle-mute player])
          :extra-classes "pl-3 pr-2"]
         (when @show-slider?
           [:input.absolute.w-24.ml-2.m-1.bottom-16
            {:class    (clojure.string/join " " styles)
             :type     "range"
             :on-input #(rf/dispatch [::events/change-volume-level (.. % -target -value) player])
             :max      100
             :value    volume-level}])]))))
