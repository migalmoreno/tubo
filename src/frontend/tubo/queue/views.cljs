(ns tubo.queue.views
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   ["react-virtuoso" :refer [Virtuoso]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.bg-player.views :as bg-player]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defn item-metadata
  [{:keys [uploader-name name service-id] :as item} i]
  (let [queue-pos @(rf/subscribe [:queue/position])]
    [:div.flex.cursor-pointer.px-4.py-2
     {:class    (into
                 (when (= i queue-pos)
                   ["bg-neutral-300/70" "dark:bg-neutral-800/60"])
                 ["h-[4.5rem]" "@sm:h-fit" "@sm:pl-0"])
      :on-click #(rf/dispatch [:queue/load-pos i])}
     [:div.items-center.justify-center.min-w-16.w-16.xs:min-w-24.xs:w-24.hidden
      {:class "@sm:flex"}
      [:span.font-bold.text-neutral-400.text-sm
       (cond
         (= i queue-pos) [:i.fa-solid.fa-play]
         :else           (inc i))]]
     [:div.flex.items-center.shrink-0.grow-0
      [layout/thumbnail item nil :container-classes
       ["h-12" "@sm:h-16" "w-16" "@sm:w-24" "@md:w-32"] :image-classes
       ["rounded"]]]
     [:div.flex.flex-col.pl-4.pr-12.w-full
      [:h1.line-clamp-1.w-fit.text-sm
       {:title name :class "@lg:text-lg"} name]
      [:div.text-neutral-600.dark:text-neutral-400.text-xs.flex.flex-col
       {:class "@lg:text-sm @lg:flex-row"}
       [:span.line-clamp-1 {:title uploader-name} uploader-name]
       (when service-id
         [:<>
          [:span.px-2.hidden
           {:dangerouslySetInnerHTML {:__html "&bull;"}
            :style                   {:font-size "0.5rem"}
            :class                   "@lg:inline-block"}]
          [:span (utils/get-service-name service-id)]])]]]))

(defn popover
  [{:keys [uploader-url uploader-name uploader-verified? uploader-avatars]
    :as   item} i]
  [:div.absolute.right-0.top-0.min-h-full.flex.items-center
   [layout/popover
    [{:label    "Start radio"
      :icon     [:i.fa-solid.fa-tower-cell]
      :on-click #(rf/dispatch [:bg-player/start-radio item])}
     {:label    "Add to playlist"
      :icon     [:i.fa-solid.fa-plus]
      :on-click #(rf/dispatch [:modals/open [modals/add-to-bookmark item]])}
     {:label    "Remove from queue"
      :icon     [:i.fa-solid.fa-trash]
      :on-click #(rf/dispatch [:queue/remove i])}
     (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
       {:label    "Unsubscribe from channel"
        :icon     [:i.fa-solid.fa-user-minus]
        :on-click #(rf/dispatch [:subscriptions/remove uploader-url])}
       {:label    "Subscribe to channel"
        :icon     [:i.fa-solid.fa-user-plus]
        :on-click #(rf/dispatch [:subscriptions/add
                                 {:url       uploader-url
                                  :name      uploader-name
                                  :verified? uploader-verified?
                                  :avatars   uploader-avatars}])})
     {:label    "Show channel details"
      :icon     [:i.fa-solid.fa-user]
      :on-click #(rf/dispatch [:navigation/navigate
                               {:name   :channel-page
                                :params {}
                                :query  {:url uploader-url}}])}]
    :tooltip-classes ["right-5" "top-0" "z-20"]
    :extra-classes ["px-4" "@sm:px-7" :py-2]]])

(defn queue-metadata
  [{:keys [name uploader-name]}]
  [:div.flex.flex-col.py-2.justify-center.items-center.gap-y-4
   [:h1.text-xl.line-clamp-1.w-fit.font-bold {:title name} name]
   [:h1.text-sm.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit.font-semibold
    {:title uploader-name}
    uploader-name]])

(defn main-controls
  [color]
  (let [loop-playback    @(rf/subscribe [:player/loop])
        shuffle?         @(rf/subscribe [:player/shuffled])
        !player          @(rf/subscribe [:bg-player])
        loading?         @(rf/subscribe [:bg-player/loading])
        waiting?         @(rf/subscribe [:bg-player/waiting])
        bg-player-ready? @(rf/subscribe [:bg-player/ready])
        paused?          @(rf/subscribe [:player/paused])
        !elapsed-time    @(rf/subscribe [:elapsed-time])
        queue            @(rf/subscribe [:queue])
        queue-pos        @(rf/subscribe [:queue/position])]
    [:div.flex.flex-col.gap-y-4
     [:div.flex.flex-auto.py-2.w-full.items-center.text-sm.gap-x-4
      [:span.whitespace-nowrap.w-16.flex.justify-end
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [bg-player/time-slider !player !elapsed-time color]
      [:span.whitespace-nowrap.w-16.flex.justify-start
       (if (and bg-player-ready? @!player)
         (utils/format-duration (.-duration @!player))
         "--:--")]]
     [:div.flex.justify-center.items-center.gap-x-6
      [player/loop-button loop-playback color true]
      [player/button
       :icon [:i.fa-solid.fa-backward-step]
       :on-click #(rf/dispatch [:queue/change-pos (dec queue-pos)])
       :disabled? (not (and queue (not= queue-pos 0)))
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon [:i.fa-solid.fa-backward]
       :on-click #(rf/dispatch [:bg-player/seek (- @!elapsed-time 5)])
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon
       (if (and (not loading?)
                (not waiting?)
                (or (nil? bg-player-ready?) @!player))
         (if paused?
           [:i.fa-solid.fa-play]
           [:i.fa-solid.fa-pause])
         [layout/loading-icon color "text-[2.5rem]"])
       :on-click
       #(rf/dispatch [:bg-player/pause (not (.-paused @!player))])
       :show-on-mobile? true
       :extra-classes ["text-[2.5rem]" "w-[2.5rem]" "flex" "justify-center"]]
      [player/button
       :icon [:i.fa-solid.fa-forward]
       :on-click #(rf/dispatch [:bg-player/seek (+ @!elapsed-time 5)])
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/button
       :icon [:i.fa-solid.fa-forward-step]
       :on-click #(rf/dispatch [:queue/change-pos (inc queue-pos)])
       :disabled? (not (and queue (< (inc queue-pos) (count queue))))
       :extra-classes [:text-xl]
       :show-on-mobile? true]
      [player/shuffle-button shuffle? color true]]]))

(defn virtualized-queue
  []
  (let [!virtuoso @(rf/subscribe [:virtuoso])]
    (r/create-class
     {:component-did-mount #(rf/dispatch [:queue/scroll-to-pos])
      :reagent-render
      (fn []
        (let [bookmarks @(rf/subscribe [:bookmarks])
              queue     @(rf/subscribe [:queue])]
          [:> Virtuoso
           {:totalCount  (count queue)
            :ref         #(reset! !virtuoso %)
            :itemContent (fn [i]
                           (let [item (get queue i)]
                             (r/as-element
                              [:div.relative.w-full
                               [item-metadata item i]
                               [popover item i bookmarks]])))}]))})))

(defn queue
  []
  (let [!active-tab (r/atom :queue)]
    (fn []
      (let [show-queue  @(rf/subscribe [:queue/show])
            show-list?  @(rf/subscribe [:queue/show-list])
            stream      @(rf/subscribe [:queue/current])
            color       (-> stream
                            :service-id
                            utils/get-service-color)
            !thumbnail  @(rf/subscribe [:queue-thumbnail])
            !bg         @(rf/subscribe [:queue-bg])
            dark-theme? @(rf/subscribe [:dark-theme])
            breakpoint  @(rf/subscribe [:layout/breakpoint])
            bg-color    (str "rgba("
                             (if dark-theme? "10,10,10" "255,255,255")
                             ","
                             "var(--opacity)"
                             ")")
            bg-image    (str "linear-gradient("
                             bg-color
                             ","
                             bg-color
                             "),url("
                             (:thumbnail stream)
                             ")")]
        [:> AnimatePresence
         (when show-queue
           [:> motion.div
            {:initial {:opacity 0}
             :animate {:opacity 1}
             :exit    {:opacity 0}
             :class   ["fixed" "flex" "flex-col" "items-center" "justify-center"
                       "z-10" "right-0" "left-0" "top-0" "bottom-0"]}
            [:> motion.div
             {:style {"--bg-image" bg-image
                      "--bg-color" (if dark-theme? "#000000" "#FFFFFF")
                      "--scale"    3
                      "--opacity"  (if dark-theme? 0.8 0.6)
                      "--blur"     "50px"}
              :ref   #(reset! !bg %)
              :class ["flex" "w-full" "h-full" "relative" "overflow-hidden"
                      "before:absolute"
                      "before:top-0" "before:bottom-0" "before:left-0"
                      "before:right-0" "before:bg-cover" "before:bg-center"
                      "before:bg-no-repeat" "before:bg-[image:var(--bg-image)]"
                      "before:bg-[color:var(--bg-color)]"
                      "before:content-['']" "before:scale-[var(--scale)]"
                      "before:blur-[--blur]"]}
             (when (and show-queue (or (= breakpoint :lg) (not show-list?)))
               [:div.flex.flex-col.flex-1.p-4.sm:p-8.items-center.justify-center.relative
                [:div.flex.flex-col.gap-y-10.items-center.justify-center
                 {:class ["w-full" "md:w-[28rem]" "lg:w-[24rem]"
                          "xl:w-[28rem]"]}
                 [:> motion.div
                  {:class   ["flex" "w-full" "items-center" "justify-center"]
                   :animate {:scale 1}
                   :ref     #(reset! !thumbnail %)
                   :initial {:scale 0.9}}
                  [layout/thumbnail stream nil :hide-label? true
                   :container-classes
                   ["h-[18rem]" "w-[18rem]" "xs:h-[24rem]" "xs:w-[24rem]"
                    "md:h-[28rem]" "md:w-full" "lg:h-[24rem]" "xl:h-[28rem]"
                    "shadow-xl" "shadow-neutral-500" "dark:shadow-neutral-900"
                    "rounded-md"]
                   :image-classes ["rounded-md"]]]
                 [:div.flex.flex-col.py-4.shrink-0.w-full.gap-y-4
                  [queue-metadata stream]
                  [main-controls color]]]])
             (when (and show-queue (or (= breakpoint :lg) show-list?))
               [:> motion.div
                {:animate    {:y 0}
                 :initial    {:y 50}
                 :transition {:duration 0.3}
                 :class      ["flex" "flex-col" "flex-1" "relative" "lg:p-8"
                              "mt-[56px]"]}
                [layout/tabs
                 [{:id    :queue
                   :label "UP NEXT"}
                  {:id    :related
                   :label "RELATED"}]
                 :selected-id @!active-tab
                 :on-change #(reset! !active-tab %)]
                [:div.flex.flex-col.h-full.w-full.gap-y-1.relative.scroll-smooth.overflow-y-auto
                 {:class "@container"}
                 (case @!active-tab
                   :queue   [virtualized-queue]
                   :related [:div.flex.flex-col.gap-y-4.p-4
                             (for [[i item] (map-indexed vector
                                                         (:related-streams
                                                          stream))]
                               ^{:key i}
                               [items/list-item-content item
                                :author-classes ["line-clamp-1" "text-xs"]
                                :title-classes
                                ["font-semibold" "line-clamp-2" "text-xs"]
                                :metadata-classes ["text-xs"]
                                :thumbnail-classes
                                ["h-[5.5rem]" "min-w-[150px]"
                                 "max-w-[150px]"]])])]])]])]))))
