(ns tubo.queue.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.player.components :as player]
   [tubo.stream.views :as stream]
   [tubo.ui :as ui]
   [tubo.utils :as utils]))

(defn queue-metadata
  [{:keys [name uploader-name]}]
  [:div.flex.flex-col.gap-y-2
   [:h1.text-xl.line-clamp-1.w-fit.font-semibold {:title name} name]
   [:h1.text-sm.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit.font-medium
    {:title uploader-name}
    uploader-name]])

(defn button
  [& {:as args}]
  [player/button
   (update args :extra-classes #(when % (concat % ["py-3"])))])

(defn main-controls
  [color]
  (let [!player          @(rf/subscribe [:bg-player])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        !paused          @(rf/subscribe [:player/paused])
        !elapsed-time    @(rf/subscribe [:elapsed-time])
        queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])]
    [:div.flex.flex-col.flex-1.justify-between
     [:div.flex.flex-col.flex-auto.w-full.items-center.gap-y-4.text-neutral-600.dark:text-neutral-300.font-medium.justify-center
      {:class "text-[0.8rem]"}
      [player/time-slider !player !elapsed-time :height "0.4rem"
       :progress-color
       color :rounded? true :thumb-color color :extra-classes
       ["[&::-webkit-slider-thumb]:-mt-1"]]
      [:div.flex.w-full.justify-between
       [:span.whitespace-nowrap.w-16.flex.justify-start
        (if (and bg-player-ready? @!player @!elapsed-time)
          (utils/format-duration @!elapsed-time)
          "--:--")]
       [:span.whitespace-nowrap.w-16.flex.justify-end
        (if (and bg-player-ready? @!player)
          (utils/format-duration (.-duration @!player))
          "--:--")]]]
     [:div.flex.justify-between.items-center.flex-auto
      [button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/previous])
       :disabled? (not (and queue (not= queue-pos 0)))
       :extra-classes ["@sm:text-xl"]
       :show-on-mobile? true]
      [button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])
       :extra-classes ["@sm:text-xl"]
       :show-on-mobile? true]
      [button
       :icon
       (if (and (not waiting?) (or (nil? bg-player-ready?) @!player))
         (if @!paused
           [:i.fa-solid.fa-play-circle]
           [:i.fa-solid.fa-pause-circle])
         [ui/loading-icon color ["text-[3.5rem]"]])
       :on-click
       #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes
       ["text-[3.5rem]" "flex" "justify-center" "!bg-transparent"]]
      [button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])
       :extra-classes ["@sm:text-xl"]
       :show-on-mobile? true]
      [button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/next])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))
       :extra-classes ["@sm:text-xl"]
       :show-on-mobile? true]]]))

(defn queue-list
  []
  (let [!active-tab (r/atom :queue)]
    (fn [stream]
      [:div
       {:class ["flex" "flex-1" "relative" "items-center" "shrink-0"
                "flex-auto" "h-full"]}
       [:div.flex.flex-auto.flex-col.h-full
        [ui/tabs
         [{:id    :queue
           :label "UP NEXT"}
          {:id    :related
           :label "RELATED"}]
         :selected-id @!active-tab
         :on-change #(reset! !active-tab %)]
        [:div.flex.flex-col.h-full.w-full.gap-y-1.relative.scroll-smooth.overflow-y-auto
         {:class "@container"}
         (case @!active-tab
           :queue   [stream/virtualized-queue]
           :related [:div.flex.flex-col.gap-y-2.p-4
                     (for [[i item]
                           (map-indexed vector
                                        (:related-items stream))]
                       ^{:key i}
                       [ui/list-item-content item
                        :container-classes
                        ["flex" "flex-col" "flex-auto" "gap-y-1"]
                        :author-classes ["line-clamp-1" "text-xs"]
                        :title-classes
                        ["font-semibold" "line-clamp-2" "text-xs"]
                        :metadata-classes ["text-xs" "gap-y-2"]
                        :thumbnail-container-classes
                        ["h-[5.5rem]" "min-w-[150px]"
                         "max-w-[150px]"]])])]]])))

(defn queue
  []
  (let [show-queue  @(rf/subscribe [:queue/show])
        stream      @(rf/subscribe [:queue/current])
        color       (-> stream
                        :service-id
                        utils/get-service-color)
        !thumbnail  @(rf/subscribe [:queue-thumbnail])
        !bg         @(rf/subscribe [:queue-bg])
        dark-theme? @(rf/subscribe [:dark-theme])
        !player     @(rf/subscribe [:bg-player])
        muted?      @(rf/subscribe [:player/muted])]
    [:> AnimatePresence
     (when show-queue
       [:> (.-div motion)
        {:initial {:opacity 0}
         :animate {:opacity 1}
         :exit    {:opacity 0}
         :class   ["fixed" "flex" "flex-col" "items-center" "z-10"
                   "justify-center" "right-0" "left-0" "top-0" "bottom-0"]}
        [:> (.-div motion)
         {:style {"--bg-color"    (or (:thumbnail-color stream)
                                      (str "rgba("
                                           (if dark-theme?
                                             "10,10,10"
                                             "245,245,245")
                                           ")"))
                  "--bg-gradient" (str
                                   "rgba("
                                   (if dark-theme? "10,10,10" "245,245,245")
                                   ",0.5)")}
          :ref   #(reset! !bg %)
          :class ["flex" "justify-center" "w-full" "h-full"
                  "relative" "overflow-hidden"
                  "bg-[color:var(--bg-color)]" "transition-colors"
                  "duration-500" "before:absolute" "before:top-0"
                  "before:bottom-0" "before:left-0" "before:right-0"
                  "before:bg-[color:var(--bg-gradient)]"
                  "before:content-['']"]}
         [:div.flex.justify-center.w-full.overflow-y-auto
          {:class ["h-[calc(100%-56px)]" "mt-[56px]"]}
          [:div.flex.gap-x-4.sm:gap-x-8.xs:px-6.my-auto
           {:class ["w-full" "xl:w-4/5" "h-full"
                    "sm:h-[600px]" "md:h-[800px]"
                    "max-h-[calc(100dvh-56px)]"]}
           [:div.flex.flex-col.relative.w-full.sm:w-fit.px-4.xs:px-0.items-center
            [:div.flex.flex-col.gap-y-4.xs:gap-y-6.flex-auto.justify-between
             {:class ["w-full" "xs:w-11/12" "sm:w-[18rem]" "md:w-[24rem]"
                      "@container"]}
             [:> (.-div motion)
              {:class   ["flex" "items-center" "justify-center"
                         "w-[min(100%,35dvh)]" "self-center"]
               :animate {:scale 1}
               :ref     #(reset! !thumbnail %)
               :initial {:scale 0.9}}
              [ui/thumbnail stream nil :hide-label? true
               :container-classes
               ["aspect-square" "min-w-full" "rounded-md"]
               :image-classes ["rounded-md"]]]
             [:div.flex.flex-col.gap-y-8.md:gap-y-16.w-full.flex-auto
              [queue-metadata stream]
              [main-controls color]]
             [:div.flex.justify-between.min-w-full.pb-4
              [player/button
               :icon
               (if muted?
                 [:i.fa-solid.fa-volume-xmark]
                 [:i.fa-solid.fa-volume-low])
               :on-click
               #(rf/dispatch [:bg-player/mute (not muted?) !player])
               :show-on-mobile? true
               :extra-classes ["text-md" "w-10"]]
              [player/shuffle-button color true :extra-classes
               ["text-md" "w-10"]]
              [player/loop-button color true :extra-classes ["text-md" "w-10"]]
              [ui/panel-popover
               [queue-list stream]
               :mobile-only? true
               :container-classes ["sm:hidden"]
               :extra-classes ["w-10"]
               :extra-panel-classes
               ["bg-neutral-300/60" "dark:bg-neutral-950/80" "backdrop-blur-xl"
                "h-[calc(100dvh-56px)]"]
               :icon [:i.fa-solid.fa-list]]]]]
           [:div.hidden.sm:flex.flex-auto
            [queue-list stream]]]]]])]))
