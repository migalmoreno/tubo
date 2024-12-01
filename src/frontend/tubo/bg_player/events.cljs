(ns tubo.bg-player.events
  (:require
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :bg-player/seek
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [db bg-player]} [_ time]]
   (when (:bg-player/ready db)
     {:player/time {:time time :player bg-player}})))

(rf/reg-event-db
 :bg-player/set-paused
 (fn [db [_ val]]
   (assoc db :player/paused val)))

(rf/reg-event-fx
 :bg-player/pause
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ paused?]]
   {:player/pause {:paused? paused?
                   :player  bg-player}}))

(rf/reg-event-fx
 :bg-player/play
 [(rf/inject-cofx ::inject/sub [:elapsed-time])
  (rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player db elapsed-time]}]
   {:fx [[:dispatch [:bg-player/set-paused false]]
         [:dispatch [:bg-player/seek @elapsed-time]]
         (when (and (:main-player/ready db) main-player @main-player)
           [:dispatch [:main-player/pause true]])]}))

(rf/reg-event-fx
 :bg-player/stop
 (fn [_]
   {:fx [[:dispatch [:bg-player/pause true]]
         [:dispatch [:bg-player/seek 0]]]}))

(rf/reg-event-fx
 :bg-player/start
 [(rf/inject-cofx ::inject/sub [:bg-player])
  (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db bg-player]} _]
   {:fx [[:dispatch [:bg-player/set-paused true]]
         [:dispatch [:bg-player/pause false]]
         [:dispatch
          [:player/change-volume (:player/volume db)
           bg-player]]
        ]}))

(rf/reg-event-fx
 :bg-player/mute
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db          (assoc db :player/muted value)
    :store       (assoc store :player/muted value)
    :player/mute {:player player :muted? value}}))

(rf/reg-event-fx
 :bg-player/hide
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   {:db    (assoc db :bg-player/show false)
    :store (assoc store :bg-player/show false)}))

(rf/reg-event-fx
 :bg-player/dispose
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [remove-entries
         (fn [elem]
           (-> elem
               (assoc :queue [])
               (assoc :queue/position 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)
      :fx    [[:dispatch [:bg-player/pause true]]
              [:dispatch [:bg-player/seek 0]]
              [:dispatch [:bg-player/hide]]]})))

(rf/reg-event-db
 :bg-player/ready
 (fn [db [_ ready]]
   (assoc db :bg-player/ready ready)))

(rf/reg-event-fx
 :bg-player/load-related-streams
 (fn [_ [_ res]]
   (let [{:keys [related-streams]} (js->clj res :keywordize-keys true)]
     {:fx [[:dispatch [:queue/add-n related-streams]]]})))

(rf/reg-event-fx
 :bg-player/fetch-related-streams
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:bg-player/load-related-streams]] [:bad-response]]]
    :db (assoc db :bg-player/loading true)}))

(rf/reg-event-fx
 :bg-player/show
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream notify?]]
   (let [updated-db (update db :queue conj stream)
         idx        (.indexOf (:queue updated-db) stream)]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    [[:dispatch
               [:bg-player/fetch-stream
                (:url stream) idx (= (count (:queue db)) 0)]]
              (when (and notify? (not (= (count (:queue db)) 0)))
                [:dispatch
                 [:notifications/add
                  {:status-text "Added stream to queue"
                   :failure     :info}]])]})))

(rf/reg-event-fx
 :bg-player/start-radio
 (fn [{:keys [db]} [_ stream]]
   {:fx [[:dispatch [:bg-player/show stream]]
         (when (not= (count (:queue db)) 0)
           [:dispatch [:queue/change-pos (count (:queue db))]])
         [:dispatch [:bg-player/fetch-related-streams (:url stream)]]
         [:dispatch
          [:notifications/add
           {:status-text "Started stream radio"
            :failure     :info}]]]}))

(rf/reg-event-fx
 :bg-player/switch-to-main
 [(rf/inject-cofx :store)]
 (fn [{:keys [db]} _]
   {:fx            [[:dispatch [:main-player/show true]]]
    :db            (assoc db :bg-player/show false)
    :scroll-to-top nil}))

(rf/reg-event-fx
 :bg-player/switch-from-main
 (fn [{:keys [db]} _]
   {:db (assoc db :bg-player/show true)
    :fx [[:dispatch [:main-player/show false]]
         [:dispatch [:main-player/pause true]]]}))

(rf/reg-event-fx
 :bg-player/load-stream
 [(rf/inject-cofx :store)
  (rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [db store bg-player]} [_ idx play? res]]
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
                         :player      bg-player}]]))})))

(rf/reg-event-fx
 :bg-player/bad-response
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
 :bg-player/fetch-stream
 (fn [{:keys [db]} [_ url idx play?]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:bg-player/load-stream idx play?]
           [:bg-player/bad-response idx play?]]]]
    :db (assoc db :bg-player/loading play?)}))
