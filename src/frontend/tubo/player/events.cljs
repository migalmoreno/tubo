(ns tubo.player.events
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [tubo.player.utils :as putils]
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

(rf/reg-fx
 :player/configure
 (fn [player]
   (.configure (.-api @player)
               (clj->js
                {"preferredVideoCodecs" ["av01" "vp9" "avc1"]
                 "preferredAudioCodecs" ["opus" "mp4a"]
                 "manifest"             {"disableVideo" false}
                 "streaming"            {"retryParameters"
                                         {"maxAttempts"   js/Infinity
                                          "baseDelay"     250
                                          "backoffFactor" 1.5}}}))))

(rf/reg-fx
 :player/request-filter
 (fn [[player url]]
   (when-let [networking-engine (.getNetworkingEngine (.-api @player))]
     (.registerRequestFilter
      networking-engine
      (fn [_ request]
        (let [original-url (some->> (.-uris request)
                                    first
                                    (new js/URL))
              proxied-url  (when (or (str/ends-with? (.-host original-url)
                                                     ".googlevideo.com")
                                     (str/ends-with? (.-host original-url)
                                                     ".bcbits.com"))
                             (new js/URL original-url))]
          (set! (.-retryParameters request)
                (clj->js {"maxAttempts"   js/Infinity
                          "baseDelay"     250
                          "backoffFactor" 1.5}))
          (when proxied-url
            (when (seq (.-Range (.-headers request)))
              (.. proxied-url
                  -searchParams
                  (set "range"
                       (get (str/split (.-Range (.-headers request)) #"=") 1)))
              (set! (.-headers request) #js {}))
            (aset (.-uris request)
                  0
                  (str url
                       "/proxy/"
                       (js/encodeURIComponent
                        (.toString proxied-url)))))))))))


(rf/reg-event-fx
 :player/set-stream
 (fn [{:keys [db]} [_ stream player pos]]
   (when stream
     (let [video-stream (putils/get-video-stream stream (:settings db))]
       {:fx [[:dispatch
              [:player/set-src
               {:player      player
                :src         video-stream
                :current-pos pos}]]]}))))

(rf/reg-event-fx
 :player/initialize
 (fn [{:keys [db]} [_ stream player pos]]
   {:fx [[:player/configure player]
         [:player/request-filter [player (get-in db [:settings :instance])]]
         [:dispatch [:player/set-stream stream player pos]]]}))

(rf/reg-event-fx
 :player/start
 (fn [_ [_ player stream]]
   {:fx [[:set-media-session-metadata
          {:title   (:name stream)
           :artist  (:uploader-name stream)
           :artwork [{:src (:thumbnail stream)}]}]
         [:set-media-session-handlers player]]}))

(rf/reg-fx
 :player/load
 (fn [[player url]]
   (.load (.-api @player) url)))

(rf/reg-event-fx
 :shaka/play-error
 (fn [{:keys [db]} [_ error player stream]]
   {:fx [[:dispatch
          [:notifications/error
           (or (seq (.-detail error)) "Playback failed. Retrying...")]]
         [:player/load
          [player (putils/get-video-stream stream (:settings db))]]]}))

(rf/reg-fx
 :player/loop
 (fn [{:keys [player loop]}]
   (set! (.-loop @player) loop)))

(rf/reg-fx
 :player/time
 (fn [{:keys [time player]}]
   (set! (.-currentTime @player) time)))

(rf/reg-event-fx
 :player/seek
 (fn [_ [_ time player]]
   {:player/time {:time time :player player}}))

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
 :set-media-session-metadata
 (fn [metadata]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-metadata js/navigator.mediaSession)
           (js/MediaMetadata. (clj->js metadata))))))

(rf/reg-fx
 :clear-media-session
 (fn [_]
   (.setPositionState js/navigator.mediaSession)))

(rf/reg-fx
 :set-media-session-handlers
 (fn [player]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [current-time (and player @player (.-currentTime @player))
           update-position
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     current-time})
           update-playback #(set! (.-playbackState js/navigator.mediaSession) %)
           seek #(do (rf/dispatch [:player/seek % player]) (update-position))
           events
           {"play"          #(do (.play @player) (update-playback "playing"))
            "pause"         #(do (.pause @player) (update-playback "paused"))
            "previoustrack" #(rf/dispatch [:queue/previous])
            "nexttrack"     #(rf/dispatch [:queue/next])
            "seekbackward"  (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (- (.-currentTime @player)
                                       (or (.-seekOffset details) 10))))
            "seekforward"   (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (+ (.-currentTime @player)
                                       (or (.-seekOffset details) 10))))
            "seekto"        (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (.-seekTime details)))
            "stop"          #(seek 0)}]
       (doseq [[action cb] events]
         (try
           (.setActionHandler js/navigator.mediaSession action cb)
           (catch js/Error _
             (js/console.error (str "The media session action "
                                    action
                                    " is not supported.")))))))))

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
