(ns tubo.player.events
  (:require
   [goog.object :as gobj]
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-fx
 :volume
 (fn [{:keys [player volume]}]
   (when @player
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 :mute
 (fn [{:keys [player muted?]}]
   (when @player
     (set! (.-muted @player) muted?))))

(rf/reg-fx
 :src
 (fn [{:keys [player src]}]
   (set! (.-source @player) (clj->js src))))

(rf/reg-fx
 :loop
 (fn [{:keys [player loop]}]
   (set! (.-loop @player) loop)))

(rf/reg-fx
 :current-time
 (fn [{:keys [time player]}]
   (set! (.-currentTime @player) time)))

(rf/reg-event-fx
 :bg-player/seek
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ time]]
   (when (:bg-player/ready db)
     {:current-time {:time time :player player}})))

(rf/reg-event-fx
 :main-player/seek
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ time]]
   {:current-time {:time time :player main-player}}))

(rf/reg-fx
 :pause!
 (fn [{:keys [paused? player]}]
   (when @player
     (set! (.-paused @player) paused?))))

(rf/reg-event-db
 :bg-player/set-paused
 (fn [db [_ val]]
   (assoc db :paused val)))

(rf/reg-event-fx
 :bg-player/pause
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [player]} [_ paused?]]
   {:pause! {:paused? paused?
             :player  player}}))

(rf/reg-event-fx
 :main-player/pause
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]} [_ paused?]]
   (when (:main-player/ready db)
     {:pause! {:paused? paused?
               :player  main-player}})))

(rf/reg-event-fx
 :bg-player/play
 [(rf/inject-cofx ::inject/sub [:elapsed-time])
  (rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db elapsed-time main-player]}]
   {:fx [[:dispatch [:bg-player/set-paused false]]
         [:dispatch [:bg-player/seek @elapsed-time]]
         (when (and (:main-player/ready db) @main-player)
           [:dispatch [:main-player/pause true]])]}))

(rf/reg-event-fx
 :main-player/play
 [(rf/inject-cofx ::inject/sub [:elapsed-time])
  (rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]}]
   {:fx [(when (and (:bg-player/ready db) @player)
           [:dispatch [:bg-player/pause true]])]}))

(rf/reg-event-fx
 :bg-player/stop
 (fn [_]
   {:fx [[:dispatch [:bg-player/pause true]]
         [:dispatch [:bg-player/seek 0]]]}))

(rf/reg-event-fx
 :bg-player/start
 [(rf/inject-cofx ::inject/sub [:player])
  (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db player]} _]
   {:fx [[:dispatch [:bg-player/set-paused true]]
         [:dispatch [:bg-player/pause false]]
         [:dispatch [:player/change-volume (:volume-level db) player]]]}))

(rf/reg-event-fx
 :main-player/start
 [(rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db elapsed-time]} _]
   {:fx [[:dispatch [:main-player/pause false]]
         (when (and (:main-player/show db) (not (:bg-player/ready db)))
           [:dispatch [:main-player/seek @elapsed-time]])]}))

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
     (let [current-time (and @player (.-currentTime @player))
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
   {:db     (assoc db :volume-level value)
    :store  (assoc store :volume-level value)
    :volume {:player player :volume value}}))

(rf/reg-event-fx
 :bg-player/mute
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db    (assoc db :muted value)
    :store (assoc store :muted value)
    :mute  {:player player :muted? value}}))

(rf/reg-event-fx
 :bg-player/hide
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   {:db    (assoc db :bg-player/show false)
    :store (assoc store :bg-player/show false)}))

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
 :bg-player/dispose
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [remove-entries
         (fn [elem]
           (-> elem
               (assoc :queue [])
               (assoc :queue-pos 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)
      :fx    [[:dispatch [:bg-player/pause true]]
              [:dispatch [:bg-player/seek 0]]
              [:dispatch [:bg-player/hide]]]})))

(rf/reg-event-db
 :bg-player/ready
 (fn [db [_ ready]]
   (assoc db :bg-player/ready ready)))

(rf/reg-event-db
 :main-player/ready
 (fn [db [_ ready]]
   (assoc db :main-player/ready ready)))

(rf/reg-event-fx
 :player/switch-to-background
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream notify?]]
   (let [updated-db (update db :queue conj stream)
         idx        (.indexOf (:queue updated-db) stream)]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    [[:dispatch
               [:player/fetch-stream
                (:url stream) idx (= (count (:queue db)) 0)]]
              (when (and notify? (not (= (count (:queue db)) 0)))
                [:dispatch
                 [:notifications/add
                  {:status-text "Added stream to queue"
                   :failure     :info}]])]})))

(rf/reg-event-fx
 :player/show-main-player
 (fn [{:keys [db]} [_ val]]
   {:db            (assoc db :main-player/show val)
    :body-overflow val}))

(rf/reg-event-fx
 :player/switch-from-main
 [(rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db]} _]
   {:db (assoc db :bg-player/show true)
    :fx [[:dispatch [:player/show-main-player false]]
         [:dispatch [:main-player/pause true]]]}))

(rf/reg-event-fx
 :player/switch-to-main
 [(rf/inject-cofx :store)]
 (fn [{:keys [db]} _]
   {:fx            [[:dispatch [:player/show-main-player true]]]
    :db            (assoc db :bg-player/show false)
    :scroll-to-top nil}))

(rf/reg-event-fx
 :player/load-related-streams
 (fn [_ [_ res]]
   (let [{:keys [related-streams]} (js->clj res :keywordize-keys true)]
     {:fx [[:dispatch [:queue/add-n related-streams]]]})))

(rf/reg-event-fx
 :player/load-stream
 [(rf/inject-cofx :store)
  (rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db store player]} [_ idx play? res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db    (assoc db
                    :bg-player/show    (not (:main-player/show db))
                    :bg-player/loading false)
      :store (assoc store :bg-player/show (not (:main-player/show db)))
      :fx    (apply conj
                    [(when play?
                       [:dispatch [:queue/change-stream stream-res idx]])]
                    (when (and (:bg-player/ready db) play?)
                      [[:media-session-metadata
                        {:title   (:name stream-res)
                         :artist  (:uploader-name stream-res)
                         :artwork [{:src (:thumbnail-url stream-res)}]}]
                       [:media-session-handlers
                        {:current-pos idx
                         :player      player}]]))})))

(rf/reg-event-fx
 :player/bad-response
 (fn [{:keys [db]} [_ idx play? res]]
   {:db (assoc db
               :bg-player/loading
               false)
    :fx [[:dispatch [:bad-response res]]
         (when play?
           (if (> (-> db
                      :queue
                      count)
                  1)
             [:dispatch [:queue/change-pos (inc idx)]]
             [:dispatch [:bg-player/dispose]]))]}))

(rf/reg-event-fx
 :player/fetch-related-streams
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:player/load-related-streams]] [:bad-response]]]
    :db (assoc db :bg-player/loading true)}))

(rf/reg-event-fx
 :player/fetch-stream
 (fn [{:keys [db]} [_ url idx play?]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:player/load-stream idx play?]
           [:player/bad-response idx play?]]]]
    :db (assoc db :bg-player/loading play?)}))

(rf/reg-event-fx
 :player/start-radio
 (fn [{:keys [db]} [_ stream]]
   {:fx [[:dispatch [:player/switch-to-background stream]]
         (when (not= (count (:queue db)) 0)
           [:dispatch [:queue/change-pos (count (:queue db))]])
         [:dispatch [:player/fetch-related-streams (:url stream)]]
         [:dispatch
          [:notifications/add
           {:status-text "Started stream radio"
            :failure     :info}]]]}))

(rf/reg-event-db
 :main-player/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:queue (:queue-pos db) layout]
    (not (get-in db [:queue (:queue-pos db) layout])))))
