(ns tubo.components.player
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   ["@vidstack/react" :refer (MediaPlayer MediaProvider Poster)]
   ["@vidstack/react/player/layouts/default" :refer (defaultLayoutIcons DefaultVideoLayout DefaultAudioLayout)]))

(defn get-player-sources
  [available-streams]
  (map (fn [{:keys [content]}] {:src content :type "video/mp4"}) (reverse available-streams)))

(defn video-player
  [stream !player]
  (let [!elapsed-time @(rf/subscribe [:elapsed-time])
        !main-player-first? (r/atom true)]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:main-player/ready false])
      :reagent-render
      (fn [{:keys [name video-streams audio-streams thumbnail-url]} !player]
        (let [show-main-player? @(rf/subscribe [:main-player/show])]
          [:> MediaPlayer
           {:title          name
            :src            (get-player-sources (into video-streams audio-streams))
            :poster         thumbnail-url
            :class          "w-full xl:w-3/5 overflow-hidden"
            :playsInline    true
            :ref            #(reset! !player %)
            :loop           (when show-main-player? (= @(rf/subscribe [:loop-playback]) :stream))
            :onSeeked       (when show-main-player?
                              #(reset! !elapsed-time (.-currentTime @!player)))
            :onTimeUpdate   (when show-main-player?
                              #(reset! !elapsed-time (.-currentTime @!player)))
            :onEnded        #(when show-main-player?
                               (rf/dispatch [:queue/change-pos (inc @(rf/subscribe [:queue-pos]))])
                               (reset! !elapsed-time 0))
            :onLoadedData   (fn []
                              (when show-main-player?
                                (rf/dispatch [:main-player/start]))
                              (when (and @!main-player-first? show-main-player?)
                                (reset! !main-player-first? false)))
            :onPlay         #(rf/dispatch [:main-player/play])
            :onCanPlay      #(rf/dispatch [:main-player/ready true])
            :onSourceChange #(when-not @!main-player-first?
                               (reset! !elapsed-time 0))}
           [:> MediaProvider
            [:> Poster {:src   thumbnail-url
                        :alt   name
                        :class :vds-poster}]]
           [:> DefaultVideoLayout {:icons defaultLayoutIcons}]]))})))

(defn audio-player
  [stream !player]
  (let [!elapsed-time     @(rf/subscribe [:elapsed-time])
        !bg-player-first? (r/atom nil)]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:background-player/ready false])
      :reagent-render
      (fn [{:keys [name audio-streams thumbnail-url]} !player]
        [:> MediaPlayer
         {:title          name
          :class          "invisible fixed"
          :controls       []
          :src            (get-player-sources audio-streams)
          :viewType       "audio"
          :ref            #(reset! !player %)
          :loop           (= @(rf/subscribe [:loop-playback]) :stream)
          :onCanPlay      #(rf/dispatch [:background-player/ready true])
          :onSeeked       #(reset! !elapsed-time (.-currentTime @!player))
          :onTimeUpdate   #(reset! !elapsed-time (.-currentTime @!player))
          :onEnded        (fn []
                            (rf/dispatch [:queue/change-pos (inc @(rf/subscribe [:queue-pos]))])
                            (reset! !elapsed-time 0))
          :onPlay         #(rf/dispatch [:background-player/play])
          :onReplay       (fn []
                            (rf/dispatch [:background-player/set-paused false])
                            (reset! !elapsed-time 0))
          :onPause        #(rf/dispatch [:background-player/set-paused true])
          :onLoadedData   (fn []
                            (rf/dispatch [:background-player/start])
                            (when-not @!bg-player-first?
                              (reset! !bg-player-first? true)))
          :onSourceChange #(when @!bg-player-first?
                             (reset! !elapsed-time 0))}
         [:> MediaProvider]
         [:> DefaultAudioLayout {:icons defaultLayoutIcons}]])})))

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

(defn time-slider [!player !elapsed-time service-color]
  (let [styles (concat base-slider-classes
                       (get-slider-bg-classes service-color)
                       (get-slider-shadow-classes service-color))
        bg-player-ready? @(rf/subscribe [:background-player/ready])]
    [:input.w-full
     {:class     styles
      :type      "range"
      :on-input  #(reset! !elapsed-time (.. % -target -value))
      :on-change #(when (and bg-player-ready? @!player) (set! (.-currentTime @!player) @!elapsed-time))
      :max       (if (and bg-player-ready? @!player (not (js/isNaN (.-duration @!player))))
                   (.floor js/Math (.-duration @!player))
                   100)
      :value     @!elapsed-time}]))

(defn button
  [& {:keys [icon on-click disabled? show-on-mobile? extra-classes]}]
  [:button.outline-none.focus:ring-transparent.px-2.pt-1
   {:class (into (into (when disabled? [:opacity-50 :cursor-auto])
                       (when-not show-on-mobile? [:hidden :lg:block]))
                  extra-classes)
    :on-click on-click}
   icon])

(defn loop-button
  [loop-playback color show-on-mobile?]
  [button
   :icon [:div.relative.flex.items-center
          [:i.fa-solid.fa-repeat
           {:style {:color (when loop-playback color)}}]
          (when (= loop-playback :stream)
            [:div.absolute.w-full.h-full.flex.justify-center.items-center.font-bold
             {:class "text-[6px]"
              :style {:color (when loop-playback color)}}
             "1"])]
   :on-click #(rf/dispatch [:player/loop])
   :extra-classes [:text-sm]
   :show-on-mobile? show-on-mobile?])

(defn volume-slider
  [player volume-level muted? service-color]
  (let [show-slider? (r/atom nil)]
    (fn [player volume-level muted? service-color]
      (let [styles (concat ["rotate-[270deg]"]
                           base-slider-classes
                           (get-slider-bg-classes service-color)
                           (get-slider-shadow-classes service-color))]
        [:div.relative.flex.flex-col.justify-center.items-center
         {:on-mouse-over #(reset! show-slider? true)
          :on-mouse-out  #(reset! show-slider? false)}
         [button
          :icon (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
          :on-click #(rf/dispatch [:background-player/mute (not muted?) player])
          :extra-classes [:pl-3 :pr-2]]
         (when @show-slider?
           [:input.absolute.w-24.ml-2.m-1.bottom-16
            {:class    (clojure.string/join " " styles)
             :type     "range"
             :on-input #(rf/dispatch [:player/change-volume (.. % -target -value) player])
             :max      100
             :value    volume-level}])]))))
