(ns tubo.queue.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :queue/show
 (fn [{:keys [db]} [_ show?]]
   {:db            (assoc db :show-queue show?)
    :body-overflow show?}))

(rf/reg-event-fx
 :queue/add
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream notify?]]
   (let [updated-db (update db :queue conj stream)]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    (if notify?
               [[:dispatch [:notifications/add
                            {:status-text "Added stream to queue"
                             :failure     :info}]]]
               [])})))

(rf/reg-event-fx
 :queue/add-n
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ streams notify?]]
   {:db    (assoc db :background-player/show (not (:main-player/show db)))
    :store (assoc store :background-player/show (not (:main-player/show db)))
    :fx    (into (map (fn [stream] [:dispatch [:queue/add stream]]) streams)
                 [[:dispatch [:player/fetch-stream (-> streams first :url)
                              (count (:queue db)) (= (count (:queue db)) 0)]]
                  (when notify?
                    [:dispatch [:notifications/add
                                {:status-text (str "Added " (count streams)
                                                   " streams to queue")
                                 :failure     :info}]])])}))

(rf/reg-event-fx
 :queue/remove
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ pos]]
   (let [updated-db   (update db :queue #(into (subvec % 0 pos) (subvec % (inc pos))))
         queue-pos    (:queue-pos db)
         queue-length (count (:queue updated-db))]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    (cond
               (and (not (= queue-length 0))
                    (or (< pos queue-pos)
                        (= pos queue-pos)
                        (= queue-pos queue-length)))
               [[:dispatch [:queue/change-pos
                            (cond
                              (= pos queue-length) 0
                              (= pos queue-pos)    pos
                              :else                (dec queue-pos))]]]
               (= (count (:queue updated-db)) 0)
               [[:dispatch [:player/dispose]]
                [:dispatch [:queue/show false]]]
               :else [])})))

(rf/reg-event-fx
 :queue/change-pos
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ i]]
   (let [idx    (if (< i (count (:queue db)))
                  i
                  (when (= (:loop-playback db) :playlist) 0))
         stream (get (:queue db) idx)]
     (when stream
       {:db    (-> db
                   (assoc :player-ready false)
                   (assoc :queue-pos idx)
                   (assoc-in [:queue idx :stream] ""))
        :store (assoc store :queue-pos idx)
        :fx    [[:dispatch [:player/fetch-stream (:url stream) idx true]]]}))))

(rf/reg-event-fx
 :queue/change-stream-source
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ src idx]]
   (let [update-entry #(assoc-in % [:queue idx :stream] src)]
     {:db    (update-entry db)
      :store (update-entry store)})))
