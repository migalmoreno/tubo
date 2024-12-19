(ns tubo.main-player.events
  (:require
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :main-player/seek
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ time]]
   {:player/time {:time time :player main-player}}))

(rf/reg-event-fx
 :main-player/pause
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]} [_ paused?]]
   (when (:main-player/ready db)
     {:player/pause {:paused? (not paused?)
                     :player  main-player}})))

(rf/reg-event-fx
 :main-player/play
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]}]
   {:fx [(when (and (:bg-player/ready db) main-player @main-player)
           [:dispatch [:bg-player/pause true]])]}))

(rf/reg-event-fx
 :main-player/start
 [(rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db elapsed-time]}]
   {:fx [[:dispatch [:main-player/pause false]]
         (when (and (:main-player/show db) (not (:bg-player/ready db)))
           [:dispatch [:main-player/seek @elapsed-time]])]}))

(rf/reg-event-db
 :main-player/ready
 (fn [db [_ ready]]
   (assoc db :main-player/ready ready)))

(rf/reg-event-db
 :main-player/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:queue (:queue/position db) layout]
    (not (get-in db [:queue (:queue/position db) layout])))))

(rf/reg-event-fx
 :main-player/show
 (fn [{:keys [db]} [_ val]]
   {:db            (apply assoc
                          (assoc db :main-player/show val)
                          (when val [:search/show-form false]))
    :body-overflow val}))
