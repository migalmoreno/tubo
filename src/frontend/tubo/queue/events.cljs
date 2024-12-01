(ns tubo.queue.events
  (:require
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :queue/show
 (fn [{:keys [db]} [_ show?]]
   {:db            (apply assoc
                          (assoc db :queue/show show?)
                          (when show? [:search/show-form false]))
    :body-overflow show?}))

(rf/reg-event-fx
 :queue/shuffle
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ val]]
   (let [queue             (:queue db)
         queue-pos         (+ 1 (:queue/position db))
         queue-to-end      (subvec queue queue-pos)
         shuffled-to-end   (shuffle queue-to-end)
         unshuffled-to-end (into (subvec queue 0 queue-pos) queue-to-end)
         shuffled-queue    (into (subvec queue 0 queue-pos) shuffled-to-end)
         unshuffled-queue  (or (:queue/unshuffled db)
                               (into (subvec queue 0 (- queue-pos 1))
                                     unshuffled-to-end))
         updated-db        (assoc db
                                  :queue
                                  (if val shuffled-queue unshuffled-queue))]
     {:db    (assoc updated-db
                    :player/shuffled  val
                    :queue/unshuffled unshuffled-queue)
      :store (assoc store :queue (:queue updated-db) :player/shuffled val)})))

(rf/reg-event-fx
 :queue/add
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream notify?]]
   (let [updated-db (update db :queue conj stream)]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    (if notify?
               [[:dispatch
                 [:notifications/add
                  {:status-text "Added stream to queue"
                   :failure     :info}]]]
               [])})))

(rf/reg-event-fx
 :queue/add-n
 [(rf/inject-cofx :store)]
 (fn [{:keys [db]} [_ streams notify?]]
   {:fx (into (map (fn [stream] [:dispatch [:queue/add stream]]) streams)
              [[:dispatch
                [:bg-player/fetch-stream
                 (-> streams
                     first
                     :url)
                 (count (:queue db)) (= (count (:queue db)) 0)]]
               (when notify?
                 [:dispatch
                  [:notifications/add
                   {:status-text (str "Added "
                                      (count streams)
                                      " streams to queue")
                    :failure     :info}]])])}))

(rf/reg-event-fx
 :queue/remove
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ pos]]
   (let [updated-db   (update db
                              :queue
                              #(into (subvec % 0 pos) (subvec % (inc pos))))
         queue-pos    (:queue/position db)
         queue-length (count (:queue updated-db))]
     {:db    updated-db
      :store (assoc store :queue (:queue updated-db))
      :fx    (cond
               (and (not (= queue-length 0))
                    (or (< pos queue-pos)
                        (= pos queue-pos)
                        (= queue-pos queue-length)))
               [[:dispatch
                 [:queue/change-pos
                  (cond
                    (= pos queue-length) 0
                    (= pos queue-pos)    pos
                    :else                (dec queue-pos))]]]
               (= (count (:queue updated-db)) 0)
               [[:dispatch [:bg-player/dispose]]
                [:dispatch [:queue/show false]]]
               :else [])})))

(rf/reg-event-fx
 :queue/change-pos
 [(rf/inject-cofx :store)]
 (fn [{:keys [db]} [_ i]]
   (let [idx    (if (< i (count (:queue db)))
                  i
                  (when (= (:player/loop db) :playlist) 0))
         stream (get (:queue db) idx)]
     (when stream
       {:fx [[:dispatch [:bg-player/fetch-stream (:url stream) idx true]]]}))))

(rf/reg-event-fx
 :queue/change-stream
 [(rf/inject-cofx :store)
  (rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [db store bg-player]} [_ stream idx]]
   (let [update-entry (fn [x] (update-in x [:queue idx] #(merge % stream)))]
     {:db         (assoc (update-entry db) :queue/position idx)
      :store      (assoc (update-entry store) :queue/position idx)
      :player/src {:player      bg-player
                   :src         (-> stream
                                    :audio-streams
                                    first
                                    :content)
                   :current-pos idx}})))
