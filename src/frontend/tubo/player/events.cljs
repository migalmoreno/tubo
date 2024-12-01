(ns tubo.player.events
  (:require
   [goog.object :as gobj]
   [re-frame.core :as rf]))

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
 (fn [{:keys [player src]}]
   (set! (.-source @player) (clj->js src))))

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
     (set! (.-paused @player) paused?))))

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
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db            (assoc db :player/volume value)
    :store         (assoc store :player/volume value)
    :player/volume {:player player :volume value}}))

(rf/reg-event-fx
 :player/loop
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [loop-state (case (:player/loop db)
                      :stream   false
                      :playlist :stream
                      :playlist)]
     {:db    (assoc db :player/loop loop-state)
      :store (assoc store :player/loop loop-state)})))
