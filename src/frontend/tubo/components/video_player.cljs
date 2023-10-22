(ns tubo.components.video-player
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   ["video.js" :as videojs]))

(defn player
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
        [:video-js.vjs-default-skin.vjs-big-play-centered.bottom-0.object-cover.min-h-full.max-h-full.min-w-full.focus:ring-transparent])})))
