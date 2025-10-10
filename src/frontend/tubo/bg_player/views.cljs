(ns tubo.bg-player.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.bookmarks.modals :as modals]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defonce slider-classes
  ["h-[var(--height)]" "cursor-pointer" "appearance-none" "rounded-full"
   "overflow-hidden" "bg-neutral-300" "dark:bg-neutral-600" "focus:outline-none"
   "[&::-webkit-slider-runnable-track]:h-[var(--height)]"
   "[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,#8c8c8c_var(--buffered),#D4D4D4_var(--buffered))]"
   "dark:[&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,#737373_var(--buffered),#525252_var(--buffered))]"
   "[&::-webkit-slider-thumb]:appearance-none"
   "[&::-webkit-slider-thumb]:border-0"
   "[&::-webkit-slider-thumb]:rounded"
   "[&::-webkit-slider-thumb]:h-[var(--height)]"
   "[&::-webkit-slider-thumb]:w-2"
   "[&::-webkit-slider-thumb]:shadow-[-405px_0_0_400px]"
   "[&::-webkit-slider-thumb]:shadow-[var(--thumb-bg)]"
   "[&::-webkit-slider-thumb]:bg-[var(--thumb-bg)]"
   "[&::-moz-range-track]:h-[var(--height)]"
   "[&::-moz-range-track]:bg-[linear-gradient(to_right,#8c8c8c_var(--buffered),#D4D4D4_var(--buffered))]"
   "dark:[&::-moz-range-track]:bg-[linear-gradient(to_right,#737373_var(--buffered),#525252_var(--buffered))]"
   "[&::-moz-range-thumb]:border-0"
   "[&::-moz-range-thumb]:rounded"
   "[&::-moz-range-thumb]:h-[var(--height)]"
   "[&::-moz-range-thumb]:w-2"
   "[&::-moz-range-thumb]:shadow-[-405px_0_0_400px]"
   "[&::-moz-range-thumb]:shadow-[var(--thumb-bg)]"
   "[&::-moz-range-thumb]:bg-[var(--thumb-bg)]"])

(defn time-slider
  [!player !elapsed-time]
  (let [bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !buffered        @(rf/subscribe [:bg-player/buffered])
        max-value        (if (and bg-player-ready?
                                  @!player
                                  (not (js/isNaN (.-duration @!player))))
                           (.floor js/Math (.-duration @!player))
                           100)]
    [:input.w-full
     {:style     {"--buffered" (str @!buffered "%")
                  "--height"   "0.5rem"}
      :class     slider-classes
      :type      "range"
      :on-click  #(.stopPropagation %)
      :on-input  #(reset! !elapsed-time (.. % -target -value))
      :on-change #(when (and bg-player-ready? @!player)
                    (set! (.-currentTime @!player) @!elapsed-time))
      :max       max-value
      :value     @!elapsed-time}]))

(defn volume-slider
  [player]
  (let [volume @(rf/subscribe [:player/volume])]
    [:input.w-full
     {:style    {"--height" "0.375rem"}
      :class    slider-classes
      :type     "range"
      :on-click #(.stopPropagation %)
      :on-input #(rf/dispatch [:player/change-volume (.. % -target -value)
                               player])
      :max      100
      :value    volume}]))

(defn metadata
  [{:keys [name uploader-name] :as stream}]
  [:div.flex.lg:flex-1.gap-x-4
   [:div
    [layout/thumbnail (dissoc stream :duration) nil :container-classes
     ["h-12" "w-12"] :image-classes ["rounded"]]]
   [:div.flex.flex-col.pr-4.gap-y-1
    [:h1.text-sm.line-clamp-1.w-fit
     {:title name}
     name]
    [:h1.text-xs.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit
     {:title uploader-name}
     uploader-name]]])

(defn main-controls
  [!player color]
  (let [queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])
        loading?         @(rf/subscribe [:bg-player/loading])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        paused?          @(rf/subscribe [:player/paused])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !elapsed-time    @(rf/subscribe [:elapsed-time])
        dark-theme       @(rf/subscribe [:dark-theme])]
    [:div.flex.flex-col.items-center.ml-auto.gap-y-1
     [:div.flex.justify-end.gap-x-6.items-center
      [player/loop-button color false :extra-classes ["text-sm"]]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/previous])
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
           [:i.fa-solid.fa-play-circle]
           [:i.fa-solid.fa-pause-circle])
         [layout/loading-icon color "text-3xl lg:text-4xl"])
       :on-click #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes ["text-3xl" "lg:text-4xl" "w-[3rem]" "lg:w-[2.5rem]"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/next])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))]
      [player/shuffle-button color false :extra-classes ["text-sm"]]]
     [:div.hidden.lg:flex.items-center.gap-x-2
      {:class "text-[0.8rem]"}
      [:span.w-16.flex.justify-end
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [:div.w-20.lg:w-64.mx-2.flex.items-center
       {:style {"--thumb-bg" (if dark-theme
                               "rgb(212,212,212)"
                               "rgb(0,0,0)")}}
       [time-slider !player !elapsed-time color]]
      [:span.w-16.flex.justify-start
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]]))

(defn popover
  [{:keys [uploader-url uploader-name uploader-verified uploader-avatars]
    :as   stream} & {:keys [tooltip-classes extra-classes]}]
  (let [queue     @(rf/subscribe [:queue])
        queue-pos @(rf/subscribe [:queue/position])
        bookmark  #(rf/dispatch [:modals/open
                                 [modals/add-to-bookmark %]])]
    [layout/popover
     [{:label             "Start radio"
       :icon              [:i.fa-solid.fa-tower-cell]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:bg-player/start-radio
                                         stream])}
      {:label             "Add current to playlist"
       :icon              [:i.fa-solid.fa-plus]
       :stop-propagation? true
       :on-click          #(bookmark stream)}
      {:label             "Add queue to playlist"
       :icon              [:i.fa-solid.fa-list]
       :stop-propagation? true
       :on-click          #(bookmark queue)}
      {:label             "Remove from queue"
       :icon              [:i.fa-solid.fa-trash]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:queue/remove
                                         queue-pos])}
      {:label             "Switch to main"
       :icon              [:i.fa-solid.fa-display]
       :stop-propagation? true
       :on-click          #(rf/dispatch
                            [:bg-player/switch-to-main])}
      (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
        {:label             "Unsubscribe from channel"
         :icon              [:i.fa-solid.fa-user-minus]
         :stop-propagation? true
         :on-click          #(rf/dispatch [:subscriptions/remove uploader-url])}
        {:label             "Subscribe to channel"
         :icon              [:i.fa-solid.fa-user-plus]
         :stop-propagation? true
         :on-click          #(rf/dispatch [:subscriptions/add
                                           {:url      uploader-url
                                            :name     uploader-name
                                            :verified uploader-verified
                                            :avatars  uploader-avatars}])})
      {:label             "Show channel details"
       :icon              [:i.fa-solid.fa-user]
       :stop-propagation? true
       :on-click          #(rf/dispatch [:navigation/navigate
                                         {:name   :channel-page
                                          :params {}
                                          :query  {:url
                                                   uploader-url}}])}
      {:label             "Close player"
       :stop-propagation? true
       :icon              [:i.fa-solid.fa-close]
       :on-click          #(rf/dispatch [:bg-player/dispose])}]
     :stop-propagation? true
     :tooltip-classes (or tooltip-classes ["right-5" "bottom-5"])
     :extra-classes
     (or extra-classes ["px-5"])]))

(defn extra-controls
  [!player]
  (let [muted?     @(rf/subscribe [:player/muted])
        dark-theme @(rf/subscribe [:dark-theme])]
    [:div.flex.lg:justify-end.lg:flex-1.gap-x-2
     [:div.hidden.lg:flex.w-36.items-center.gap-x-4
      {:style {"--thumb-bg"
               (if dark-theme "rgb(212,212,212)" "rgb(0,0,0)")}}
      [player/button
       :icon
       (if muted? [:i.fa-solid.fa-volume-xmark] [:i.fa-solid.fa-volume-low])
       :on-click #(rf/dispatch [:bg-player/mute (not muted?) !player])
       :show-on-mobile? true
       :extra-classes ["w-6"]]
      [volume-slider !player]]
     [player/button
      :icon [:i.fa-solid.fa-up-right-and-down-left-from-center]
      :on-click #(rf/dispatch [:queue/show true])
      :show-on-mobile? true
      :extra-classes
      ["text-lg" "lg:text-base" "!pl-6" "!pr-4" "hidden" "lg:block"]]]))

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
  [!player]
  (let [!elapsed  @(rf/subscribe [:elapsed-time])
        pos       @(rf/subscribe [:queue/position])
        stream    @(rf/subscribe [:queue/current])
        !buffered @(rf/subscribe [:bg-player/buffered])]
    (r/create-class
     {:component-will-unmount #(rf/dispatch [:bg-player/unmount])
      :component-did-mount #(rf/dispatch [:bg-player/mount stream !player pos])
      :reagent-render
      (fn [!player]
        (let [stream @(rf/subscribe [:queue/current])]
          [:audio
           {:ref            #(reset! !player %)
            :preload        "metadata"
            :on-waiting     #(rf/dispatch [:bg-player/set-waiting true])
            :loop           (= @(rf/subscribe [:player/loop]) :stream)
            :muted          @(rf/subscribe [:player/muted])
            :on-can-play    #(rf/dispatch [:bg-player/set-ready true])
            :on-seeked      #(reset! !elapsed (.-currentTime @!player))
            :on-progress    #(on-progress !player !buffered)
            :on-time-update #(on-update !player !buffered !elapsed)
            :on-loaded-data #(rf/dispatch [:bg-player/start])
            :on-play        #(rf/dispatch [:bg-player/play !player stream])
            :on-error       #(rf/dispatch [:player/on-error !player])
            :on-pause       #(rf/dispatch [:bg-player/set-paused true])}]))})))

(defn player
  []
  (let [!player      @(rf/subscribe [:bg-player])
        stream       @(rf/subscribe [:queue/current])
        show-queue?  @(rf/subscribe [:queue/show])
        show-player? @(rf/subscribe [:bg-player/show])
        color        (-> stream
                         :service-id
                         utils/get-service-color)]
    [:<>
     (when show-player?
       [audio-player !player])
     [:> AnimatePresence
      (when (and show-player? (not show-queue?))
        [:> (.-div motion)
         {:animate    {:y 0}
          :initial    {:y 100}
          :transition {:ease "easeOut" :duration 0.3}
          :exit       {:y 100}
          :class      ["h-[80px]" "sticky" "flex" "items-center" "left-0"
                       "right-0" "bottom-0" "z-10" "p-3" "relative"
                       "cursor-pointer" "bg-neutral-200" "dark:bg-neutral-900"]
          :on-click   #(rf/dispatch [:queue/show true])}
         [metadata stream]
         [main-controls !player color]
         [extra-controls !player]])]]))
