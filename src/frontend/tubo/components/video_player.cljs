(ns tubo.components.video-player
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   ["video.js" :as videojs]
   ["videojs-mobile-ui"]
   ["@silvermine/videojs-quality-selector" :as VideojsQualitySelector]))

(defn player
  [options]
  (let [!player         (atom nil)
        service-color   @(rf/subscribe [:service-color])
        {:keys [theme]} @(rf/subscribe [:settings])]
    (r/create-class
     {:display-name "VideoPlayer"
      :component-did-mount
      (fn [^videojs/VideoJsPlayer this]
        (let [set-bg-color! #(set! (.. (.$ (.getChild ^videojs/VideoJsPlayer @!player "ControlBar") %)
                                       -style
                                       -background)
                                   service-color)]
          (VideojsQualitySelector videojs)
          (reset! !player (videojs (rdom/dom-node this) (clj->js options)))
          (set-bg-color! ".vjs-play-progress")
          (set-bg-color! ".vjs-volume-level")
          (set-bg-color! ".vjs-slider-bar")
          (.ready @!player #(.mobileUi ^videojs/VideoJsPlayer @!player))
          (.on @!player "play" (fn []
                                 (.audioPosterMode
                                  @!player
                                  (clojure.string/includes?
                                   (:label (first (filter #(= (:src %) (.src @!player))
                                                          (:sources options))))
                                   "audio-only"))))))
      :component-will-unmount #(when @!player (.dispose @!player))
      :reagent-render
      (fn [options]
        [:video-js.vjs-tubo.vjs-default-skin.vjs-big-play-centered.vjs-show-big-play-button-on-pause])})))
