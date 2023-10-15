(ns tubo.components.player
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.loading :as loading]
   [tubo.events :as events]
   [tubo.util :as util]
   ["video.js" :as videojs]))

(defn global-player
  []
  (let [!player (r/atom nil)
        !loop? (r/atom nil)
        !elapsed-time (r/atom 0)
        !volume-level (r/atom 100)]
    (fn []
      (let [media-queue @(rf/subscribe [:media-queue])
            media-queue-pos @(rf/subscribe [:media-queue-pos])
            {:keys [uploader-name uploader-url name stream url service-color]}
            (and (not-empty media-queue) (nth media-queue media-queue-pos))
            show-global-player? @(rf/subscribe [:show-global-player])
            show-global-player-loading? @(rf/subscribe [:show-global-player-loading])]
        (when show-global-player?
          [:div.sticky.bottom-0.z-50.bg-white.dark:bg-neutral-900.px-3.py-5.sm:p-5.absolute.box-border.m-0
           {:style {:borderColor service-color :borderTopWidth "2px" :borderStyle "solid"}}
           [:div.flex.items-center.justify-between
            [:div.flex.items-center
             [:div.flex.flex-col
              [:a.text-xs.line-clamp-1
               {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
              [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300.line-clamp-1
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
             [:audio
              {:src            stream :ref #(reset! !player %)
               :loop           @!loop?
               :autoPlay       true
               :on-time-update #(and @!player (> (.-readyState @!player) 0)
                                     (reset! !elapsed-time (.-currentTime @!player)))
               :on-ended       #(and (< (+ media-queue-pos 1) (count media-queue))
                                     (rf/dispatch [::events/change-media-queue-pos
                                                   (+ media-queue-pos 1)]))}]]
            [:div.mx-2.flex
             (when (and media-queue (not= media-queue-pos 0))
               [:button.focus:outline-none.mx-2
                {:on-click #(rf/dispatch [::events/change-media-queue-pos
                                          (- media-queue-pos 1)])}
                [:i.fa-solid.fa-backward-step]])
             [:button.focus:outline-none.mx-2
              {:on-click #(when-let [player @!player]
                            (if (.-paused player)
                              (.play player)
                              (.pause player)))}
              (if @!player
                (if show-global-player-loading?
                  [loading/loading-icon service-color "text-1xl"]
                  (if (.-paused @!player)
                    [:i.fa-solid.fa-play]
                    [:i.fa-solid.fa-pause]))
                [:i.fa-solid.fa-play])]
             (when (and media-queue (< (+ media-queue-pos 1) (count media-queue)))
               [:button.focus:ring-transparent.mx-2
                {:on-click #(rf/dispatch [::events/change-media-queue-pos
                                          (+ media-queue-pos 1)])}
                [:i.fa-solid.fa-forward-step]])
             [:div.flex
              [:div.mx-2.hidden.sm:flex
               [:span (if @!elapsed-time (util/format-duration @!elapsed-time) "00:00")]
               [:span.mx-2 "/"]
               [:span (when (and @!player (> (.-readyState @!player) 0))
                        (util/format-duration (.-duration @!player)))]]
              [:input.mx-2.w-20.ml:w-80.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none
               {:type      "range"
                :on-input  #(reset! !elapsed-time (.. % -target -value))
                :on-change #(and @!player (> (.-readyState @!player) 0)
                                 (set! (.-currentTime @!player) @!elapsed-time))
                :style     {:accentColor service-color}
                :max       (if (and @!player (> (.-readyState @!player) 0))
                             (.floor js/Math (.-duration @!player))
                             100)
                :value     @!elapsed-time}]
              [:button.focus:ring-transparent.mx-2
               {:on-click (fn [] (swap! !loop? #(not %)))}
               [:i.fa-solid.fa-repeat
                {:style {:color (when @!loop? service-color)}}]]
              [:div.mx-2.flex
               [:button.focus:outline-none.mx-2
                {:on-click #(when-let [player @!player]
                              (set! (.-muted player) (not (.-muted player))))}
                (if (and @!player (.-muted @!player))
                  [:i.fa-solid.fa-volume-xmark]
                  [:i.fa-solid.fa-volume-low])]
               [:input.w-20.bg-gray-200.rounded-lg.cursor-pointer.focus:outline-none.hidden.sm:block
                {:type "range"
                 :on-input #(do (reset! !volume-level (.. % -target -value))
                                (and @!player (> (.-readyState @!player) 0)
                                     (set! (.-volume @!player) (/ @!volume-level 100))))
                 :style {:accentColor service-color}
                 :max 100
                 :value @!volume-level}]]]
             [:div.ml-2
              [:i.fa-solid.fa-close.cursor-pointer
               {:on-click (fn []
                            (rf/dispatch [::events/toggle-global-player])
                            (.pause @!player))}]]]]])))))

(defn stream-player
  [options url]
  (let [!player (atom nil)]
    (r/create-class
     {:display-name "StreamPlayer"
      :component-did-mount
      (fn [this]
        (reset! !player (videojs (rdom/dom-node this) (clj->js options))))
      :component-did-update
      (fn [this [_ prev-argv prev-more]]
        (when (and @!player (not= prev-more (first (r/children this))))
          (.src @!player (apply array (map #(js-obj "type" % "src" (first (r/children this)))
                                           (map #(get % "type") (get options "sources")))))
          (.ready @!player #(.play @!player))))
      :component-will-unmount
      (fn [_]
        (when @!player
          (.dispose @!player)))
      :reagent-render
      (fn [options url]
        [:video-js.vjs-default-skin.vjs-big-play-centered.bottom-0.object-cover.min-h-full.max-h-full.min-w-full])})))
