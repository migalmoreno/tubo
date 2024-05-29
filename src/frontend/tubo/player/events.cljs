(ns tubo.player.events
  (:require
   [tubo.utils :as utils]
   [goog.object :as gobj]
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-fx
 :volume
 (fn [{:keys [player volume]}]
   (when (and @player (> (.-readyState @player) 0))
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 :mute
 (fn [{:keys [player muted?]}]
   (when (and @player (> (.-readyState @player) 0))
     (set! (.-muted @player) muted?))))

(rf/reg-fx
 :src
 (fn [{:keys [player src current-pos]}]
   (set! (.-src @player) src)
   (set! (.-onended @player)
         #(rf/dispatch [:queue/change-pos (inc current-pos)]))))

(rf/reg-fx
 :current-time
 (fn [{:keys [time player]}]
   (set! (.-currentTime @player) time)))

(rf/reg-event-fx
 :player/seek
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ time]]
   {:current-time {:time time :player player}}))

(rf/reg-fx
 :pause
 (fn [{:keys [paused? player]}]
   (when (and @player (> (.-readyState @player) 0))
     (if paused?
       (.play @player)
       (.pause @player)))))

(rf/reg-event-db
 :player/set-paused
 (fn [db [_ val]]
   (assoc db :paused val)))

(rf/reg-event-fx
 :player/pause
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ paused?]]
   {:pause {:paused? (not paused?)
            :player  player}}))

(rf/reg-event-fx
 :player/stop
 (fn [{:keys [db]}]
   {:fx [[:dispatch [:player/pause true]]
         [:dispatch [:player/seek 0]]]}))

(rf/reg-event-fx
 :player/start-in-background
 [(rf/inject-cofx ::inject/sub [:player])
  (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db player]} _]
   {:fx [[:dispatch [:player/set-paused true]]
         [:dispatch [:player/pause false]]
         [:dispatch [:player/change-volume (:volume-level db) player]]]
    :db (assoc db :player-ready (and @player (> (.-readyState @player) 0)))}))

(rf/reg-fx
 :audio-poster-mode
 (fn [{:keys [player options]}]
   (.audioPosterMode
    @player
    (-> (filter #(= (:src %) (.src @player)) (:sources options))
        first
        :label
        (clojure.string/includes? "audio-only")))))

(rf/reg-fx
 :slider-color
 (fn [{:keys [player color]}]
   (doseq [class [".vjs-play-progress" ".vjs-volume-level" ".vjs-slider-bar"]]
     (set! (.. (.$ (.getChild ^videojs/VideoJsPlayer @player "ControlBar") class) -style -background) color))))

(rf/reg-event-fx
 :player/set-slider-color
 (fn [_ [_ !player service-id]]
   {:slider-color {:player !player :color (utils/get-service-color service-id)}}))

(rf/reg-event-fx
 :player/start-in-main
 [(rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db]} [_ !player options service-id]]
   {:fx [[:audio-poster-mode {:player !player :options options}]]}))

(rf/reg-fx
 :media-session-metadata
 (fn [metadata]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-metadata js/navigator.mediaSession)
           (js/MediaMetadata. (clj->js metadata))))))

(rf/reg-fx
 :media-session-handlers
 (fn [{:keys [current-pos player stream]}]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [current-time (.-currentTime @player)
           update-position
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     (.-currentTime @player)})
           seek #(do (rf/dispatch [:seek %]) (update-position))
           events
           {"play"          #(.play @player)
            "pause"         #(.pause @player)
            "previoustrack" #(rf/dispatch [:change-queue-pos (dec current-pos)])
            "nexttrack"     #(rf/dispatch [:change-queue-pos (inc current-pos)])
            "seekbackward"  (fn [^js/navigator.MediaSessionActionDetails details]
                              (seek (- current-time (or (.-seekOffset details) 10))))
            "seekforward"   (fn [^js/navigator.MediaSessionActionDetails details]
                              (seek (+ current-time (or (.-seekOffset details) 10))))
            "seekto"        (fn [^js/navigator.MediaSessionActionDetails details]
                              (seek (.-seekTime details)))
            "stop"          #(seek 0)}]
       (doseq [[action cb] events]
         (.setActionHandler js/navigator.mediaSession action cb))))))

(rf/reg-event-fx
 :player/change-volume
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db     (assoc db :volume-level value)
    :store  (assoc store :volume-level value)
    :volume {:player player :volume value}}))

(rf/reg-event-fx
 :player/mute
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db    (assoc db :muted value)
    :store (assoc store :muted value)
    :mute  {:player player :muted? value}}))

(rf/reg-event-fx
 :player/loop
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [loop-state (case (:loop-playback db)
                      :stream   false
                      :playlist :stream
                      :playlist)]
     {:db    (assoc db :loop-playback loop-state)
      :store (assoc store :loop-playback loop-state)})))

(rf/reg-event-fx
 :player/dispose
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [remove-entries
         (fn [elem]
           (-> elem
               (update :show-background-player #(not %))
               (assoc :player-ready false)
               (assoc :queue [])
               (assoc :queue-pos 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)
      :fx    [[:dispatch [:player/pause true]]
              [:dispatch [:player/seek 0]]]})))

(rf/reg-event-fx
 :player/switch-to-background
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream]]
   (let [updated-db (update db :queue conj stream)
         idx        (.indexOf (:queue updated-db) stream)]
     {:db    (-> updated-db
                 (assoc :show-background-player true))
      :store (-> store
                 (assoc :show-background-player true)
                 (assoc :queue (:queue updated-db)))
      :fx    [[:dispatch [:player/fetch-stream
                          (:url stream) idx (= (count (:queue db)) 0)]]
              (when-not (= (count (:queue db)) 0)
                [:dispatch [:notifications/add
                            {:status-text (str "Added stream to queue")
                             :failure     :success}]])]})))

(rf/reg-event-fx
 :player/load-related-streams
 (fn [{:keys [db]} [_ res]]
   (let [{:keys [related-streams]} (js->clj res :keywordize-keys true)]
     {:fx [[:dispatch [:queue/add-n related-streams]]]})))

(rf/reg-event-fx
 :player/load-stream
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ idx play? res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :show-background-player-loading false)
      :fx (apply conj [[:dispatch [:queue/change-stream-source
                                   (-> stream-res :audio-streams first :content)
                                   idx]]]
                 (when play?
                   [[:src
                     {:player      player
                      :src         (-> stream-res :audio-streams first :content)
                      :current-pos (:queue-pos db)}]
                    [:media-session-metadata
                     {:title   (:name stream-res)
                      :artist  (:uploader-name stream-res)
                      :artwork [{:src (:thumbnail-url stream-res)}]}]
                    [:media-session-handlers
                     {:current-pos (:queue-pos db)
                      :player player}]]))})))

(rf/reg-event-fx
 :player/bad-response
 (fn [{:keys [db]} [_ play? res]]
   {:db (assoc db
               :show-background-player-loading false
               :player-ready true)
    :fx [[:dispatch [:bad-response res]]
         (when play?
           (if (> (-> db :queue count) 1)
             [:dispatch [:queue/change-pos (-> db :queue-pos inc)]]
             [:dispatch [:player/dispose]]))]}))

(rf/reg-event-fx
 :player/fetch-related-streams
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch [:stream/fetch url
                     [:player/load-related-streams]] [:bad-response]]]
    :db (assoc db :show-background-player-loading true)}))

(rf/reg-event-fx
 :player/fetch-stream
 (fn [{:keys [db]} [_ url idx play?]]
   {:fx [[:dispatch [:stream/fetch url
                     [:player/load-stream idx play?]
                     [:player/bad-response play?]]]]
    :db (assoc db :show-background-player-loading true)}))

(rf/reg-event-fx
 :player/start-radio
 (fn [{:keys [db]} [_ stream]]
   {:fx [[:dispatch [:player/switch-to-background stream]]
         (when (not= (count (:queue db)) 0)
           [:dispatch [:queue/change-pos (count (:queue db))]])
         [:dispatch [:player/fetch-related-streams (:url stream)]]]}))
