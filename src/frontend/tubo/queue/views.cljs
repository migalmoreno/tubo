(ns tubo.queue.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.bg-player.views :as bg-player]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(defn item-metadata
  [{:keys [uploader-name name service-id] :as item} queue-pos i !clicked-idx]
  [:div.flex.cursor-pointer.px-4.py-2
   {:class    (into
               (when (= i queue-pos)
                 ["bg-neutral-300/70" "dark:bg-neutral-800/60"])
               ["h-[4.5rem]" "@sm:h-fit" "@sm:pl-0"])
    :on-click #(do (rf/dispatch [:queue/load-pos i])
                   (reset! !clicked-idx i))}
   [:div.items-center.justify-center.min-w-16.w-16.xs:min-w-24.xs:w-24.hidden
    {:class "@sm:flex"}
    [:span.font-bold.text-neutral-400.text-sm
     (cond
       (and (= i @!clicked-idx)
            @(rf/subscribe [:bg-player/loading])
            (not= queue-pos @!clicked-idx))
       [:<>
        [:div.block.lg:hidden
         [layout/loading-icon nil :text-xl]]
        [:div.hidden.lg:block (inc i)]]
       (= i queue-pos) [:i.fa-solid.fa-play]
       :else (inc i))]]
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
        [:span (utils/get-service-name service-id)]])]]])

(defn popover
  [{:keys [uploader-url] :as item} i]
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
     {:label    "Show channel details"
      :icon     [:i.fa-solid.fa-user]
      :on-click #(rf/dispatch [:navigation/navigate
                               {:name   :channel-page
                                :params {}
                                :query  {:url uploader-url}}])}]
    :tooltip-classes ["right-5" "top-0" "z-20"]
    :extra-classes ["px-4" "@sm:px-7" :py-2]]])

(defn queue-item
  [item queue queue-pos i bookmarks !clicked-idx]
  [:div.relative.w-full
   {:ref #(when (and queue (= queue-pos i)) (rf/dispatch [:scroll-top %]))}
   [item-metadata item queue-pos i !clicked-idx]
   [popover item i bookmarks]])

(defn queue-metadata
  [{:keys [name uploader-name]}]
  [:div.flex.flex-col.py-2
   [:h1.text-xl.line-clamp-1.w-fit.font-bold {:title name} name]
   [:h1.text-sm.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1.w-fit.font-semibold
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
     [:div.flex.flex-auto.py-2.w-full.items-center.text-sm
      [:span.mr-4.whitespace-nowrap.w-8
       (if (and bg-player-ready? @!player @!elapsed-time)
         (utils/format-duration @!elapsed-time)
         "--:--")]
      [bg-player/time-slider !player !elapsed-time color]
      [:span.ml-4.whitespace-nowrap.w-8
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

(defn queue
  []
  (let [!active-tab  (r/atom :queue)
        !clicked-idx (r/atom nil)]
    (fn []
      (let [show-queue  @(rf/subscribe [:queue/show])
            show-list?  @(rf/subscribe [:queue/show-list])
            stream      @(rf/subscribe [:queue/current])
            bookmarks   @(rf/subscribe [:bookmarks])
            queue-pos   @(rf/subscribe [:queue/position])
            queue       @(rf/subscribe [:queue])
            color       (-> stream
                            :service-id
                            utils/get-service-color)
            dark-theme? @(rf/subscribe [:dark-theme])
            bg-color    (str "rgba("
                             (if dark-theme? "10,10,10" "255,255,255")
                             ","
                             (if dark-theme? 0.8 0.6)
                             ")")
            bg-image    (str "linear-gradient("
                             bg-color
                             ","
                             bg-color
                             "),url("
                             (:thumbnail stream)
                             ")")]
        [:div.fixed.flex.flex-col.items-center.justify-center.backdrop-blur.z-10.w-full.left-0.transition-all.ease-in-out.top-0.h-dvh
         {:class ["dark:bg-neutral-950/90" "bg-neutral-100/90"
                  (when-not show-queue "invisible")
                  (if show-queue "opacity-1" "opacity-0")]}
         [:div.flex.w-full.h-full.relative.overflow-hidden.before:absolute.before:top-0.before:bottom-0.before:left-0.before:right-0.before:bg-cover.before:bg-center.before:bg-no-repeat
          {:style {"--bg-image" bg-image}
           :class ["before:bg-[image:var(--bg-image)]"
                   "before:content-['']" "before:scale-[3]"
                   "before:blur-[50px]"]}
          [:div.flex-col.flex-1.gap-y-10.p-4.sm:p-8.items-center.justify-center.relative
           {:class (if show-queue
                     (if show-list?
                       ["hidden" "lg:flex"]
                       ["flex"])
                     ["hidden"])}
           [:div.flex.w-full.items-center.justify-center
            [layout/thumbnail stream nil :hide-duration? true
             :container-classes
             ["h-[18rem]" "w-[18rem]" "md:h-[24rem]" "md:w-[24rem]"
              "md:h-[28rem]" "md:w-[28rem]" "lg:h-[24rem]" "lg:w-[24rem]"
              "xl:h-[28rem]" "xl:w-[28rem]"]
             :image-classes ["rounded-md"]]]
           [:div.flex.flex-col.py-4.shrink-0.px-5.w-full.gap-y-4
            [queue-metadata stream]
            [main-controls color]]]
          [:div.flex-col.flex-1.relative.lg:p-8
           {:class (into ["mt-[56px]"]
                         (if show-queue
                           (if show-list? ["flex"] ["hidden" "lg:flex"])
                           ["hidden"]))}
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
              :queue   [:<>
                        (for [[i item] (map-indexed vector queue)]
                          ^{:key i}
                          [queue-item item queue queue-pos i bookmarks
                           !clicked-idx])]
              :related [:div.flex.flex-col.gap-y-4.p-4
                        (for [[i item] (map-indexed vector
                                                    (:related-streams stream))]
                          ^{:key i}
                          [items/list-item-content item
                           :author-classes ["line-clamp-1" "text-xs"]
                           :title-classes
                           ["font-semibold" "line-clamp-2" "text-xs"]
                           :metadata-classes ["text-xs"]
                           :thumbnail-classes
                           ["h-[5.5rem]" "min-w-[150px]"
                            "max-w-[150px]"]])])]]]]))))
