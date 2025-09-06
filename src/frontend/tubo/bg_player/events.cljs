(ns tubo.bg-player.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [tubo.player.utils :as utils]
   [tubo.storage :refer [persist]]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :bg-player/seek
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ time]]
   {:player/time {:time time :player bg-player}}))

(rf/reg-event-db
 :bg-player/set-paused
 (fn [db [_ val]]
   (assoc db :player/paused val)))

(rf/reg-event-fx
 :bg-player/pause
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ paused?]]
   {:player/pause {:paused? (not paused?)
                   :player  bg-player}}))

(rf/reg-event-db
 :bg-player/set-waiting
 (fn [db [_ val]]
   (assoc db :bg-player/waiting val)))

(rf/reg-event-fx
 :bg-player/set-src
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ src pos]]
   {:fx [[:dispatch
          [:player/set-src {:src src :player bg-player :current-pos pos}]]]}))

(rf/reg-event-fx
 :bg-player/set-stream
 (fn [{:keys [db]} [_ stream pos]]
   (when-let [audio-stream (utils/get-audio-stream stream (:settings db))]
     {:fx [[:dispatch [:bg-player/set-src audio-stream pos]]]
      :db (assoc db :bg-player/loading false)})))

(rf/reg-event-fx
 :bg-player/start
 [(rf/inject-cofx ::inject/sub [:bg-player])
  (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db bg-player elapsed-time]}]
   {:fx [[:dispatch [:bg-player/set-paused true]]
         [:dispatch [:bg-player/seek @elapsed-time]]
         [:dispatch [:bg-player/pause false]]
         [:dispatch
          [:player/change-volume (:player/volume db)
           bg-player]]]}))

(rf/reg-event-fx
 :bg-player/mute
 [persist]
 (fn [{:keys [db]} [_ value player]]
   {:db          (assoc db :player/muted value)
    :player/mute {:player player :muted? value}}))

(rf/reg-event-fx
 :bg-player/hide
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :bg-player/show false)}))

(rf/reg-event-fx
 :bg-player/dispose
 [persist]
 (fn [{:keys [db]}]
   {:db (-> db
            (assoc :queue [])
            (assoc :queue/unshuffled nil)
            (assoc :queue/position 0)
            (assoc :player/shuffled false))
    :fx [[:dispatch [:bg-player/pause true]]
         [:dispatch [:bg-player/seek 0]]
         [:dispatch [:queue/show false]]
         [:timeout
          {:id    (nano-id)
           :event [:bg-player/hide]
           :time  200}]]}))

(rf/reg-event-db
 :bg-player/ready
 (fn [db [_ ready]]
   (assoc db :bg-player/ready ready)))

(rf/reg-event-fx
 :bg-player/load-related-streams
 (fn [_ [_ {:keys [body]}]]
   {:fx [[:dispatch [:queue/add-n (:related-streams body)]]]}))

(rf/reg-event-fx
 :bg-player/fetch-related-streams
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:bg-player/load-related-streams]] [:bad-response]]]
    :db (assoc db :bg-player/loading true)}))

(rf/reg-event-fx
 :bg-player/start-radio
 (fn [{:keys [db]} [_ stream]]
   (let [updated-db (update db :queue conj stream)
         idx        (.lastIndexOf (:queue updated-db) stream)]
     {:fx [[:dispatch [:queue/add stream]]
           [:dispatch [:bg-player/fetch-stream (:url stream) idx true]]
           [:dispatch [:bg-player/fetch-related-streams (:url stream)]]
           [:dispatch
            [:notifications/add
             {:status-text "Started stream radio"
              :type        :info}]]]})))

(rf/reg-event-fx
 :bg-player/switch-to-main
 [(rf/inject-cofx ::inject/sub [:queue/current])]
 (fn [{:keys [db] :as cofx}]
   {:fx [[:dispatch [:main-player/show true]]
         [:dispatch [:queue/show false]]
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :comments-page]))
           [:dispatch
            [:comments/fetch-page (:url (:queue/current cofx))
             [:queue (:queue/position db)]]])
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :related-streams]))
           [:dispatch
            [:bg-player/fetch-stream (:url (:queue/current cofx))
             (:queue/position db) false]])]
    :db (assoc db :bg-player/show false)}))

(rf/reg-event-fx
 :bg-player/switch-from-main
 (fn [{:keys [db]}]
   {:db (assoc db :bg-player/show true)
    :fx [[:dispatch [:main-player/show false]]
         [:dispatch [:main-player/pause true]]]}))

(rf/reg-event-fx
 :bg-player/load-stream
 [persist]
 (fn [{:keys [db]} [_ idx play? {:keys [body]}]]
   {:db (assoc db
               :bg-player/show    (not (:main-player/show db))
               :bg-player/loading false)
    :fx [[:dispatch [:queue/change-stream body idx play?]]]}))

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
   (merge (when-not (nil? play?)
            {:db (assoc db :bg-player/loading play?)})
          {:fx [[:dispatch
                 [:stream/fetch url
                  [:bg-player/load-stream idx play?]
                  [:bg-player/bad-response idx play?]]]]})))
