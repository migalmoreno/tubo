(ns tubo.components.player
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.events :as events]
   ["video.js" :as videojs]))

(defn global-player
  []
  (let [!player (r/atom nil)
        !loop? (r/atom nil)]
    (fn []
      (let [{:keys [uploader-name uploader-url name stream url service-color]} @(rf/subscribe [:global-stream])
            show-global-player? @(rf/subscribe [:show-global-player])]
        (when show-global-player?
          [:div.sticky.bottom-0.z-50.bg-white.dark:bg-neutral-900.p-5.absolute.box-border.m-0
           {:style {:borderColor service-color :borderTopWidth "2px" :borderStyle "solid"}}
           [:div.flex.items-center.justify-between
            [:div.flex.items-center
             [:div.flex.flex-col
              [:a.text-xs.line-clamp-1
               {:href (rfe/href :tubo.routes/stream nil {:url url})} name]
              [:a.text-xs.pt-2.text-neutral-600.dark:text-neutral-300
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})} uploader-name]]
             [:div.px-2.py-0.md:pt-4
              [:audio {:src stream :ref #(reset! !player %) :loop @!loop?}]]
             [:div.mx-2.flex
              [:button.focus:ring-transparent.mx-2
               {:on-click (fn [] (swap! !loop? #(not %)))}
               [:i.fa-solid.fa-repeat
                {:style {:color (when @!loop? service-color)}}]]
              [:button.focus:ring-transparent.mx-2
               {:on-click #(when-let [player @!player]
                             (if (.-paused player)
                               (.play player)
                               (.pause player)))}
               (if @!player
                 (if (.-paused @!player)
                   [:i.fa-solid.fa-play]
                   [:i.fa-solid.fa-pause])
                 [:i.fa-solid.fa-play])]]]
            [:div.px-2
             [:i.fa-solid.fa-close.cursor-pointer
              {:on-click (fn []
                           (rf/dispatch [::events/toggle-global-player])
                           (.pause @!player))}]]]])))))

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
