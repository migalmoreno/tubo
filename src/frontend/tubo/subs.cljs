(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defonce is-window-visible
  (let [a (r/atom true)]
    (.addEventListener js/window "focus" #(reset! a true))
    (.addEventListener js/window "blur" #(reset! a false))
    a))

(defonce scroll-distance
  (let [a (r/atom 0)
        compute-scroll-distance #(when (> (.-scrollY js/window) 0)
                                   (reset! a (+ (.-scrollY js/window) (.-innerHeight js/window))))]
    (.addEventListener js/window "scroll" compute-scroll-distance)
    (.addEventListener js/window "touchmove" compute-scroll-distance)
    a))

(rf/reg-sub
 :is-window-visible
 (fn [_ _]
   @is-window-visible))

(rf/reg-sub
 :scrolled-to-bottom
 (fn [_ _]
   (> (+ @scroll-distance 35) (.-scrollHeight js/document.body))))

(rf/reg-sub
 :http-response
 (fn [db _]
   (:http-response db)))

(rf/reg-sub
 :search-results
 (fn [db _]
   (:search-results db)))

(rf/reg-sub
 :stream
 (fn [db _]
   (:stream db)))

(rf/reg-sub
 :stream-format
 (fn [db _]
   (:stream-format db)))

(rf/reg-sub
 :playlist
 (fn [db _]
   (:playlist db)))

(rf/reg-sub
 :channel
 (fn [db _]
   (:channel db)))

(rf/reg-sub
 :search-query
 (fn [db _]
   (:search-query db)))

(rf/reg-sub
 :service-id
 (fn [db _]
   (:service-id db)))

(rf/reg-sub
 :service-color
 (fn [_]
   (rf/subscribe [:service-id]))
 (fn [id _]
   (case id
     0 "#cc0000"
     1 "#ff7700"
     2 "#333333"
     3 "#F2690D"
     4 "#629aa9")))

(rf/reg-sub
 :services
 (fn [db _]
   (:services db)))

(rf/reg-sub
 :kiosks
 (fn [db _]
   (:kiosks db)))

(rf/reg-sub
 :kiosk
 (fn [db _]
   (:kiosk db)))

(rf/reg-sub
 :current-match
 (fn [db _]
   (:current-match db)))

(rf/reg-sub
 :page-scroll
 (fn [db _]
   (:page-scroll db)))

(rf/reg-sub
 :media-queue
 (fn [db _]
   (:media-queue db)))

(rf/reg-sub
 :media-queue-pos
 (fn [db _]
   (:media-queue-pos db)))

(rf/reg-sub
 :media-queue-stream
 (fn [_]
   [(rf/subscribe [:media-queue]) (rf/subscribe [:media-queue-pos])])
 (fn [[queue pos] _]
   (and (not-empty queue) (nth queue pos))))

(rf/reg-sub
 :show-audio-player
 (fn [db _]
   (:show-audio-player db)))

(rf/reg-sub
 :show-audio-player-loading
 (fn [db _]
   (:show-audio-player-loading db)))

(rf/reg-sub
 :show-page-loading
 (fn [db _]
   (:show-page-loading db)))

(rf/reg-sub
 :show-pagination-loading
 (fn [db _]
   (:show-pagination-loading db)))

(rf/reg-sub
 :show-mobile-nav
 (fn [db _]
   (:show-mobile-nav db)))

(rf/reg-sub
 :theme
 (fn [db _]
   (:theme db)))

(rf/reg-sub
 :settings
 (fn [db _]
   (:settings db)))

(rf/reg-sub
 :loop-file
 (fn [db _]
   (:loop-file db)))

(rf/reg-sub
 :loop-playlist
 (fn [db _]
   (:loop-playlist db)))
