(ns tubo.main-player.events
  (:require
   [re-frame.core :as rf]
   [tubo.player.utils :as utils]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :main-player/seek
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ time]]
   {:player/time {:time time :player main-player}}))

(rf/reg-event-fx
 :main-player/pause
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ paused?]]
   {:player/pause {:paused? (not paused?)
                   :player  main-player}}))

(rf/reg-event-fx
 :main-player/play
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]}]
   {:fx [(when (and (:bg-player/ready db) main-player @main-player)
           [:dispatch [:bg-player/pause true]])]}))

(rf/reg-event-fx
 :main-player/set-stream
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player db]} [_ stream pos]]
   (let [video-stream (utils/get-video-stream stream (:settings db))]
     {:fx [[:dispatch [:player/load main-player video-stream pos]]]})))

(rf/reg-event-fx
 :main-player/initialize
 [(rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db elapsed-time]} [_ stream player pos]]
   {:fx [[:dispatch [:player/initialize stream player pos]]
         [:dispatch [:main-player/pause false]]
         [:dispatch [:main-player/seek @elapsed-time]]
         [:dispatch
          [:player/change-volume (:player/volume db) player]]]}))

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
    :fx            [(when val
                      [:dispatch [:queue/scroll-to-pos]])]
    :body-overflow val}))
