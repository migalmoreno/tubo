(ns tubo.player.events
  (:require
   [goog.object :as gobj]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [tubo.player.utils :as utils]
   [tubo.storage :refer [persist]]))

(rf/reg-fx
 :player/volume
 (fn [{:keys [player volume]}]
   (when (and player @player)
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 :player/mute
 (fn [{:keys [player muted?]}]
   (when (and player @player)
     (set! (.-muted @player) muted?))))

(rf/reg-fx
 :player/src
 (fn [{:keys [player src current-pos]}]
   (when (and player @player)
     (set! (.-src @player) (clj->js src))
     (when current-pos
       (set! (.-onended @player)
             #(rf/dispatch [:queue/change-pos (inc current-pos)]))))))

(rf/reg-event-fx
 :player/set-src
 (fn [_ [_ opts]]
   {:player/src opts}))

(rf/reg-event-fx
 :player/set-stream
 (fn [{:keys [db]} [_ stream player pos]]
   (when-let [video-stream (utils/get-video-stream stream (:settings db))]
     {:fx [[:dispatch
            [:player/set-src
             {:player player :src video-stream :current-pos pos}]]]})))

(rf/reg-fx
 :player/loop
 (fn [{:keys [player loop]}]
   (set! (.-loop @player) loop)))

(rf/reg-fx
 :player/time
 (fn [{:keys [time player]}]
   (when (and player @player)
     (set! (.-currentTime @player) time))))

(rf/reg-fx
 :player/pause
 (fn [{:keys [paused? player]}]
   (when (and player @player)
     (-> (if paused?
             (.play @player)
             (.pause @player))
         (p/catch #(rf/dispatch [:player/play-error % player]))))))

(rf/reg-event-fx
 :player/play-error
 (fn [_ [_ error player]]
   {:fx (case (.-code error)
          0 []
          1 []
          9 [[:dispatch [:notifications/error "Playback failed. Retrying..."]]
             [:dispatch [:queue/reload-current-stream player]]]
          [[:dispatch [:notifications/error (.-message error)]]])}))

(rf/reg-event-fx
 :player/on-error
 (fn [_ [_ player]]
   (when (and @player (.-error @player))
     {:fx (case (.. @player -error -code)
            4 [[:dispatch [:notifications/error "Playback failed. Retrying..."]]
               [:dispatch [:queue/reload-current-stream player]]]
            [[:dispatch
              [:notifications/error (.. @player -error -message)]]])})))

(rf/reg-fx
 :media-session-metadata
 (fn [metadata]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-metadata js/navigator.mediaSession)
           (js/MediaMetadata. (clj->js metadata))))))

(rf/reg-fx
 :media-session-handlers
 (fn [{:keys [current-pos player]}]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [current-time (and player @player (.-currentTime @player))
           update-position
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     current-time})
           seek #(do (rf/dispatch [:seek %]) (update-position))
           events
           {"play"          #(.play @player)
            "pause"         #(.pause @player)
            "previoustrack" #(rf/dispatch [:queue/change-pos (dec current-pos)])
            "nexttrack"     #(rf/dispatch [:queue/change-pos (inc current-pos)])
            "seekbackward"  (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (- current-time
                                       (or (.-seekOffset details) 10))))
            "seekforward"   (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (+ current-time
                                       (or (.-seekOffset details) 10))))
            "seekto"        (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (.-seekTime details)))
            "stop"          #(seek 0)}]
       (doseq [[action cb] events]
         (.setActionHandler js/navigator.mediaSession action cb))))))

(rf/reg-event-fx
 :player/change-volume
 [persist]
 (fn [{:keys [db]} [_ value player]]
   {:db            (assoc db :player/volume value)
    :player/volume {:player player :volume value}}))

(rf/reg-event-fx
 :player/loop
 [persist]
 (fn [{:keys [db]} _]
   (let [loop-state (case (:player/loop db)
                      :stream   false
                      :playlist :stream
                      :playlist)]
     {:db (assoc db :player/loop loop-state)})))
