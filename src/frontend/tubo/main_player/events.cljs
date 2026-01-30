(ns tubo.main-player.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [tubo.main-player.views :as main-player]
   [tubo.player.utils :as utils]
   [tubo.storage :refer [persist]]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :main-player/seek
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ time]]
   {:player/time {:time time :player main-player}}))

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
 [(rf/inject-cofx ::inject/sub [:elapsed-time])
  (rf/inject-cofx ::inject/sub [:queue/current])]
 (fn [{:keys [db elapsed-time] :as cofx} [_ stream player pos]]
   {:db (assoc db :main-player/show true)
    :fx [[:dispatch [:player/initialize stream player pos]]
         [:dispatch [:main-player/seek @elapsed-time]]
         [:dispatch
          [:player/change-volume (:player/volume db) player]]
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :comments-page]))
           [:dispatch
            [:comments/fetch-page (:url (:queue/current cofx))
             [:queue (:queue/position db)]]])
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :related-items]))
           [:dispatch
            [:bg-player/fetch-stream (:url (:queue/current cofx))
             (:queue/position db) false]])]}))

(rf/reg-event-db
 :main-player/ready
 (fn [db [_ ready]]
   (assoc db :main-player/ready ready)))

(rf/reg-event-fx
 :main-player/show
 (fn []
   {:fx [[:dispatch [:queue/show false]]
         [:dispatch
          [:layout/show-mobile-panel
           {:id            (nano-id)
            :view          [main-player/player-container]
            :extra-classes ["h-[calc(100dvh-56px)]" "bg-neutral-100"
                            "dark:bg-neutral-950"]}]]]}))

(rf/reg-event-db
 :main-player/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:queue (:queue/position db) layout]
    (not (get-in db [:queue (:queue/position db) layout])))))

(rf/reg-event-fx
 :main-player/unmount
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :main-player/show false)
    :fx [(when (and (> (count (:queue db)) 0) (not (:queue/show db)))
           [:dispatch [:bg-player/show]])
         [:dispatch [:bg-player/start]]]}))
