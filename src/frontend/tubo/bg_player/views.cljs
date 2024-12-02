(ns tubo.bg-player.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defonce base-slider-classes
  ["h-2" "cursor-pointer" "appearance-none" "bg-neutral-300"
   "dark:bg-neutral-600"
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
    "#cc0000" ["[&::-webkit-slider-thumb]:shadow-[#cc0000]"
               "[&::-moz-range-thumb]:shadow-[#cc0000]"]
    "#ff7700" ["[&::-webkit-slider-thumb]:shadow-[#ff7700]"
               "[&::-moz-range-thumb]:shadow-[#ff7700]"]
    "#333333" ["[&::-webkit-slider-thumb]:shadow-[#333333]"
               "[&::-moz-range-thumb]:shadow-[#333333]"]
    "#F2690D" ["[&::-webkit-slider-thumb]:shadow-[#F2690D]"
               "[&::-moz-range-thumb]:shadow-[#F2690D]"]
    "#629aa9" ["[&::-webkit-slider-thumb]:shadow-[#629aa9]"
               "[&::-moz-range-thumb]:shadow-[#629aa9]"]
    ["[&::-webkit-slider-thumb]:shadow-neutral-300"
     "[&::-moz-range-thumb]:shadow-neutral-300"]))

(defn get-slider-bg-classes
  [service-color]
  (case service-color
    "#cc0000" ["[&::-webkit-slider-thumb]:bg-[#cc0000]"
               "[&::-moz-range-thumb]:bg-[#cc0000]"]
    "#ff7700" ["[&::-webkit-slider-thumb]:bg-[#ff7700]"
               "[&::-moz-range-thumb]:bg-[#ff7700]"]
    "#333333" ["[&::-webkit-slider-thumb]:bg-[#333333]"
               "[&::-moz-range-thumb]:bg-[#333333]"]
    "#F2690D" ["[&::-webkit-slider-thumb]:bg-[#F2690D]"
               "[&::-moz-range-thumb]:bg-[#F2690D]"]
    "#629aa9" ["[&::-webkit-slider-thumb]:bg-[#629aa9]"
               "[&::-moz-range-thumb]:bg-[#629aa9]"]
    ["[&::-webkit-slider-thumb]:bg-neutral-300"
     "[&::-moz-range-thumb]:bg-neutral-300"]))

(defn button
  [& {:keys [icon on-click disabled? show-on-mobile? extra-classes]}]
  [:button.outline-none.focus:ring-transparent
   {:class    (into (into (when disabled? [:opacity-50 :cursor-auto])
                          (when-not show-on-mobile? [:hidden :lg:block]))
                    extra-classes)
    :on-click on-click}
   icon])

(defn loop-button
  [loop-playback color show-on-mobile?]
  [button
   :icon
   [:div.relative.flex.items-center
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

(defn shuffle-button
  [shuffle? color show-on-mobile?]
  [button
   :icon
   [:i.fa-solid.fa-shuffle {:style {:color (when shuffle? color)}}]
   :on-click #(rf/dispatch [:queue/shuffle (not shuffle?)])
   :extra-classes [:text-sm]
   :show-on-mobile? show-on-mobile?])

(defn time-slider
  [!player !elapsed-time service-color]
  (let [styles           (concat base-slider-classes
                                 (get-slider-bg-classes service-color)
                                 (get-slider-shadow-classes service-color))
        bg-player-ready? @(rf/subscribe [:bg-player/ready])]
    [:input.w-full
     {:class     styles
      :type      "range"
      :on-input  #(reset! !elapsed-time (.. % -target -value))
      :on-change #(when (and bg-player-ready? @!player)
                    (set! (.-currentTime @!player) @!elapsed-time))
      :max       (if (and bg-player-ready?
                          @!player
                          (not (js/isNaN (.-duration @!player))))
                   (.floor js/Math (.-duration @!player))
                   100)
      :value     @!elapsed-time}]))

(defn volume-slider
  [_ _ _ _]
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
          :icon
          (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
          :on-click #(rf/dispatch [:bg-player/mute (not muted?) player])]
         (when @show-slider?
           [:input.absolute.w-24.ml-2.m-1.bottom-16
            {:class    (str/join " " styles)
             :type     "range"
             :on-input #(rf/dispatch [:player/change-volume
                                      (.. % -target -value) player])
             :max      100
             :value    volume-level}])]))))

(defn metadata
  [{:keys [thumbnail-url url name uploader-url uploader-name]}]
  [:div.flex.items-center.lg:flex-1
   [:div
    [layout/thumbnail thumbnail-url (rfe/href :stream-page nil {:url url})
     name nil :classes [:h-14 :py-2 "w-[70px]"]]]
   [:div.flex.flex-col.pl-2.pr-4
    [:a.text-xs.line-clamp-1
     {:href  (rfe/href :stream-page nil {:url url})
      :title name}
     name]
    [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
     {:href  (rfe/href :channel-page nil {:url uploader-url})
      :title uploader-name}
     uploader-name]]])

(defn main-controls
  [!player color]
  (let [queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])
        loading?         @(rf/subscribe [:bg-player/loading])
        loop-playback    @(rf/subscribe [:player/loop])
        shuffle?         @(rf/subscribe [:player/shuffled])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        paused?          @(rf/subscribe [:player/paused])
        !elapsed-time    @(rf/subscribe [:elapsed-time])]
    [:div.flex.flex-col.items-center.ml-auto
     [:div.flex.justify-end.gap-x-4
      [loop-button loop-playback color]
      [button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))]
      [button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])]
      [button
       :icon
       (if (and (not loading?) (or (nil? bg-player-ready?) @!player))
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon color "lg:text-2xl"])
       :on-click #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes ["lg:text-2xl"]]
      [button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])]
      [button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]
      [shuffle-button shuffle? color]]
     [:div.hidden.lg:flex.items-center.text-sm
      [:span.mx-2
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       [time-slider !player !elapsed-time color]]
      [:span.mx-2
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]]))

(defn extra-controls
  [_ _ _]
  (let [!menu-active? (r/atom nil)]
    (fn [!player {:keys [url uploader-url] :as stream} color]
      (let [muted?    @(rf/subscribe [:player/muted])
            volume    @(rf/subscribe [:player/volume])
            queue     @(rf/subscribe [:queue])
            queue-pos @(rf/subscribe [:queue/position])
            bookmarks @(rf/subscribe [:bookmarks])
            liked?    (some #(= (:url %) url)
                            (-> bookmarks
                                first
                                :items))
            bookmark  #(rf/dispatch [:modals/open [modals/add-to-bookmark %]])]
        [:div.flex.lg:justify-end.lg:flex-1.gap-x-2
         [volume-slider !player volume muted? color]
         [button
          :icon [:i.fa-solid.fa-list]
          :on-click #(rf/dispatch [:queue/show true])
          :show-on-mobile? true
          :extra-classes [:!pl-4 :!pr-3]]
         [layout/popover-menu !menu-active?
          [{:label    (if liked? "Remove favorite" "Favorite")
            :icon     [:i.fa-solid.fa-heart
                       (when liked? {:style {:color color}})]
            :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add)
                                     stream true])}
           {:label    "Start radio"
            :icon     [:i.fa-solid.fa-tower-cell]
            :on-click #(rf/dispatch [:bg-player/start-radio stream])}
           {:label    "Add current to playlist"
            :icon     [:i.fa-solid.fa-plus]
            :on-click #(bookmark stream)}
           {:label    "Add queue to playlist"
            :icon     [:i.fa-solid.fa-list]
            :on-click #(bookmark queue)}
           {:label    "Remove from queue"
            :icon     [:i.fa-solid.fa-trash]
            :on-click #(rf/dispatch [:queue/remove queue-pos])}
           {:label    "Switch to main"
            :icon     [:i.fa-solid.fa-display]
            :on-click #(rf/dispatch [:bg-player/switch-to-main])}
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigation/navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}
           {:label    "Close player"
            :icon     [:i.fa-solid.fa-close]
            :on-click #(rf/dispatch [:bg-player/dispose])}]
          :menu-styles {:bottom "30px" :top nil :right "10px"}
          :extra-classes [:!pl-4 :px-3]]]))))

(defn audio-player
  [_]
  (let [!elapsed-time @(rf/subscribe [:elapsed-time])
        queue-pos     @(rf/subscribe [:queue/position])
        stream        @(rf/subscribe [:queue/current])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:bg-player/ready false])
      :component-did-mount
      (fn [this]
        (set! (.-onended (rdom/dom-node this))
              #(rf/dispatch [:queue/change-pos (inc queue-pos)]))
        (when stream
          (set! (.-src (rdom/dom-node this))
                (:content (nth (:audio-streams stream) 0)))))
      :reagent-render
      (fn [!player]
        [:audio
         {:ref            #(reset! !player %)
          :loop           (= @(rf/subscribe [:player/loop]) :stream)
          :on-can-play    #(rf/dispatch [:bg-player/ready true])
          :on-seeked      #(reset! !elapsed-time (.-currentTime @!player))
          :on-time-update #(reset! !elapsed-time (.-currentTime @!player))
          :on-play        #(rf/dispatch [:bg-player/set-paused false])
          :on-pause       #(rf/dispatch [:bg-player/set-paused true])
          :on-loaded-data #(rf/dispatch [:bg-player/start])}])})))

(defn player
  []
  (let [!player      @(rf/subscribe [:bg-player])
        stream       @(rf/subscribe [:queue/current])
        show-queue?  @(rf/subscribe [:queue/show])
        show-player? @(rf/subscribe [:bg-player/show])
        dark-theme?  @(rf/subscribe [:dark-theme])
        color        (-> stream
                         :service-id
                         utils/get-service-color)
        bg-color     (str "rgba("
                          (if dark-theme? "23,23,23" "255,255,255")
                          ",0.95)")
        bg-image     (str "linear-gradient("
                          bg-color
                          ","
                          bg-color
                          "),url("
                          (:thumbnail-url stream)
                          ")")]
    (when show-player?
      [:div.sticky.absolute.left-0.bottom-0.z-10.p-3.transition-all.ease-in.relative
       {:style
        {:visibility          (when show-queue? "hidden")
         :opacity             (if show-queue? 0 1)
         :background-image    bg-image
         :background-size     "cover"
         :background-position "center"
         :background-repeat   "no-repeat"}}
       [:div.flex.items-center
        [audio-player !player]
        [metadata stream]
        [main-controls !player color]
        [extra-controls !player stream color]]])))
