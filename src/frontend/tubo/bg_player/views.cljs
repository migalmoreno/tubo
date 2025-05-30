(ns tubo.bg-player.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defonce slider-classes
  ["h-2" "cursor-pointer" "appearance-none" "rounded-full"
   "overflow-hidden"
   "bg-neutral-300" "dark:bg-neutral-600" "focus:outline-none"
   "[&::-webkit-slider-runnable-track]:h-2"
   "[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,#A3A3A3_var(--buffered),#D4D4D4_var(--buffered))]"
   "dark:[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,#737373_var(--buffered),#525252_var(--buffered))]"
   "[&::-webkit-slider-thumb]:appearance-none"
   "[&::-webkit-slider-thumb]:border-0"
   "[&::-webkit-slider-thumb]:rounded"
   "[&::-webkit-slider-thumb]:h-2"
   "[&::-webkit-slider-thumb]:w-2"
   "[&::-webkit-slider-thumb]:shadow-[-405px_0_0_400px]"
   "[&::-webkit-slider-thumb]:shadow-[var(--thumb-bg)]"
   "[&::-webkit-slider-thumb]:bg-[var(--thumb-bg)]"
   "[&::-moz-range-track]:h-2"
   "[&::-moz-range-track]:bg-[linear-gradient(to_right,#A3A3A3_var(--buffered),#D4D4D4_var(--buffered))]"
   "dark:[&::-moz-range-track]:bg-[linear-gradient(to_right,#737373_var(--buffered),#525252_var(--buffered))]"
   "[&::-moz-range-thumb]:border-0"
   "[&::-moz-range-thumb]:rounded"
   "[&::-moz-range-thumb]:h-2"
   "[&::-moz-range-thumb]:w-2"
   "[&::-moz-range-thumb]:shadow-[-405px_0_0_400px]"
   "[&::-moz-range-thumb]:shadow-[var(--thumb-bg)]"
   "[&::-moz-range-thumb]:bg-[var(--thumb-bg)]"])

(defn time-slider
  [!player !elapsed-time service-color]
  (let [bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !buffered        @(rf/subscribe [:bg-player/buffered])
        max-value        (if (and bg-player-ready?
                                  @!player
                                  (not (js/isNaN (.-duration @!player))))
                           (.floor js/Math (.-duration @!player))
                           100)]
    [:input.w-full
     {:style     {"--buffered" (str @!buffered "%")
                  "--thumb-bg" service-color}
      :class     slider-classes
      :type      "range"
      :on-input  #(reset! !elapsed-time (.. % -target -value))
      :on-change #(when (and bg-player-ready? @!player)
                    (set! (.-currentTime @!player) @!elapsed-time))
      :max       max-value
      :value     @!elapsed-time}]))

(defn volume-slider
  []
  (let [show-slider? (r/atom nil)]
    (fn [player volume-level muted? service-color]
      [:div.relative.flex.flex-col.justify-center.items-center
       {:on-mouse-over #(reset! show-slider? true)
        :on-mouse-out  #(reset! show-slider? false)}
       [player/button
        :icon
        (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
        :on-click #(rf/dispatch [:bg-player/mute (not muted?) player])]
       (when @show-slider?
         [:input.absolute.w-24.ml-2.m-1.bottom-16
          {:style    {"--thumb-bg" service-color}
           :class    (concat ["rotate-[270deg]"] slider-classes)
           :type     "range"
           :on-input #(rf/dispatch [:player/change-volume (.. % -target -value)
                                    player])
           :max      100
           :value    volume-level}])])))

(defn metadata
  [{:keys [url name uploader-url uploader-name] :as stream}]
  [:div.flex.items-center.lg:flex-1
   [:div
    [layout/thumbnail (dissoc stream :duration)
     (rfe/href :stream-page nil {:url url}) :classes
     [:h-14 :py-2 "w-[70px]"]]]
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
        waiting?         @(rf/subscribe [:bg-player/waiting])
        loop-playback    @(rf/subscribe [:player/loop])
        shuffle?         @(rf/subscribe [:player/shuffled])
        paused?          @(rf/subscribe [:player/paused])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !elapsed-time    @(rf/subscribe [:elapsed-time])]
    [:div.flex.flex-col.items-center.ml-auto
     [:div.flex.justify-end.gap-x-4
      [player/loop-button loop-playback color]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])]
      [player/button
       :icon
       (if (and (not loading?)
                (not waiting?)
                (or (nil? bg-player-ready?) @!player))
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon color "lg:text-2xl"])
       :on-click #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes ["lg:text-2xl"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]
      [player/shuffle-button shuffle? color]]
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

(defn popover
  [{:keys [uploader-url] :as stream}]
  (let [queue     @(rf/subscribe [:queue])
        queue-pos @(rf/subscribe [:queue/position])
        bookmark  #(rf/dispatch [:modals/open
                                 [modals/add-to-bookmark %]])]
    [layout/popover
     [{:label    "Start radio"
       :icon     [:i.fa-solid.fa-tower-cell]
       :on-click #(rf/dispatch [:bg-player/start-radio
                                stream])}
      {:label    "Add current to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(bookmark stream)}
      {:label    "Add queue to playlist"
       :icon     [:i.fa-solid.fa-list]
       :on-click #(bookmark queue)}
      {:label    "Remove from queue"
       :icon     [:i.fa-solid.fa-trash]
       :on-click #(rf/dispatch [:queue/remove
                                queue-pos])}
      {:label    "Switch to main"
       :icon     [:i.fa-solid.fa-display]
       :on-click #(rf/dispatch
                   [:bg-player/switch-to-main])}
      {:label    "Show channel details"
       :icon     [:i.fa-solid.fa-user]
       :on-click #(rf/dispatch [:navigation/navigate
                                {:name   :channel-page
                                 :params {}
                                 :query  {:url
                                          uploader-url}}])}
      {:label    "Close player"
       :icon     [:i.fa-solid.fa-close]
       :on-click #(rf/dispatch [:bg-player/dispose])}]
     :tooltip-classes ["right-5" "bottom-5"]
     :extra-classes ["px-5" "xs:p-3"]]))

(defn extra-controls
  [!player stream color]
  (let [muted? @(rf/subscribe [:player/muted])
        volume @(rf/subscribe [:player/volume])]
    [:div.flex.lg:justify-end.lg:flex-1.gap-x-2
     [volume-slider !player volume muted? color]
     [player/button
      :icon [:i.fa-solid.fa-list]
      :on-click #(rf/dispatch [:queue/show true])
      :show-on-mobile? true
      :extra-classes [:!pl-4 :!pr-3]]
     [popover stream]]))

(defn on-progress
  [!player !buffered]
  (let [len (.. @!player -buffered -length)]
    (when (and (.-duration @!player) (> len 0))
      (if (= (.end (.-buffered @!player) (- len 1)) (.-duration @!player))
        (reset! !buffered 100)
        (when (< (.start (.-buffered @!player) (- len 1))
                 (.-currentTime @!player))
          (reset! !buffered (* (/ (.end (.-buffered @!player) (- len 1))
                                  (.-duration @!player))
                               100)))))))

(defn on-update
  [!player !buffered !elapsed]
  (when @(rf/subscribe [:bg-player/waiting])
    (rf/dispatch [:bg-player/set-waiting false]))
  (on-progress !player !buffered)
  (reset! !elapsed (.-currentTime @!player)))

(defn audio-player
  []
  (let [!elapsed  @(rf/subscribe [:elapsed-time])
        pos       @(rf/subscribe [:queue/position])
        stream    @(rf/subscribe [:queue/current])
        !buffered @(rf/subscribe [:bg-player/buffered])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:bg-player/ready false])
      :component-did-mount #(rf/dispatch [:bg-player/set-stream stream pos])
      :reagent-render
      (fn [!player]
        [:audio
         {:ref            #(reset! !player %)
          :preload        "metadata"
          :on-waiting     #(rf/dispatch [:bg-player/set-waiting true])
          :loop           (= @(rf/subscribe [:player/loop]) :stream)
          :muted          @(rf/subscribe [:player/muted])
          :on-can-play    #(rf/dispatch [:bg-player/ready true])
          :on-seeked      #(reset! !elapsed (.-currentTime @!player))
          :on-progress    #(on-progress !player !buffered)
          :on-time-update #(on-update !player !buffered !elapsed)
          :on-loaded-data #(rf/dispatch [:bg-player/start])
          :on-play        #(rf/dispatch [:bg-player/set-paused false])
          :on-pause       #(rf/dispatch [:bg-player/set-paused true])}])})))

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
                          (if dark-theme? "10,10,10" "255,255,255")
                          ",0.95)")
        bg-image     (str "linear-gradient("
                          bg-color
                          ","
                          bg-color
                          "),url("
                          (:thumbnail stream)
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
