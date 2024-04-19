(ns tubo.subs
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.utils :as utils]))

(defonce !is-window-visible
  (let [a (r/atom true)]
    (.addEventListener js/window "focus" #(reset! a true))
    (.addEventListener js/window "blur" #(reset! a false))
    a))

(defonce !scroll-distance
  (let [a (r/atom 0)
        compute-scroll-distance #(when (> (.-scrollY js/window) 0)
                                   (reset! a (+ (.-scrollY js/window) (.-innerHeight js/window))))]
    (.addEventListener js/window "scroll" compute-scroll-distance)
    (.addEventListener js/window "touchmove" compute-scroll-distance)
    a))

(defonce !elapsed-time (r/atom 0))
(defonce !player (atom nil))

(rf/reg-sub
 :is-window-visible
 (fn [_ _]
   @!is-window-visible))

(rf/reg-sub
 :scrolled-to-bottom
 (fn [_ _]
   (> (+ @!scroll-distance 35) (.-scrollHeight js/document.body))))

(rf/reg-sub
 :elapsed-time
 (fn [db _]
   !elapsed-time))

(rf/reg-sub
 :player
 (fn [db _]
   !player))

(rf/reg-sub
 :player-ready
 (fn [db _]
   (:player-ready db)))

(rf/reg-sub
 :paused
 (fn [db _]
   (:paused db)))

(rf/reg-sub
 :volume-level
 (fn [db _]
   (:volume-level db)))

(rf/reg-sub
 :muted
 (fn [db _]
   (:muted db)))

(rf/reg-sub
 :http-response
 (fn [db _]
   (:http-response db)))

(rf/reg-sub
 :search-results
 (fn [db _]
   (:search-results db)))

(rf/reg-sub
 :notifications
 (fn [db _]
   (:notifications db)))

(rf/reg-sub
 :modal
 (fn [db _]
   (:modal db)))

(rf/reg-sub
 :stream
 (fn [db _]
   (:stream db)))

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
   (utils/get-service-color id)))

(rf/reg-sub
 :service-name
 (fn [_]
   (rf/subscribe [:service-id]))
 (fn [id _]
   (utils/get-service-name id)))

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
 :bookmarks
 (fn [db _]
   (:bookmarks db)))

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
 :show-search-form
 (fn [db _]
   (:show-search-form db)))

(rf/reg-sub
 :show-media-queue
 (fn [db _]
   (:show-media-queue db)))

(rf/reg-sub
 :settings
 (fn [db _]
   (:settings db)))

(rf/reg-sub
 :loop-playback
 (fn [db _]
   (:loop-playback db)))
