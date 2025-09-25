(ns tubo.queue.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [tubo.storage :refer [persist]]
   [tubo.utils :as utils]
   [vimsical.re-frame.cofx.inject :as inject]))

(defn get-stream-metadata
  [stream]
  (select-keys
   stream
   [:type :service-id :url :name :thumbnail :thumbnails :audio-streams
    :video-streams :verified? :uploader-name :uploader-url
    :uploader-verified? :uploader-avatar :uploader-avatars :upload-date
    :short-description :duration :view-count :playlist-id]))

(rf/reg-event-fx
 :queue/show
 (fn [{:keys [db]} [_ show?]]
   {:db            (assoc db :queue/show show?)
    :fx            (if show?
                     [[:dispatch [:search/activate false]]]
                     [])
    :body-overflow show?}))

(rf/reg-event-fx
 :queue/show-list
 (fn [{:keys [db]} [_ value]]
   {:db (assoc db :queue/show-list value)}))

(rf/reg-event-fx
 :queue/shuffle
 [persist]
 (fn [{:keys [db]} [_ val]]
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
     {:db (assoc updated-db
                 :player/shuffled  val
                 :queue/unshuffled (if val unshuffled-queue nil))})))

(rf/reg-event-fx
 :queue/add
 [persist]
 (fn [{:keys [db]} [_ stream notify?]]
   (let [updated-db (update
                     db
                     :queue
                     conj
                     (-> stream
                         (get-stream-metadata)
                         (utils/apply-image-quality db :thumbnail :thumbnails)
                         (utils/apply-image-quality db
                                                    :uploader-avatar
                                                    :uploader-avatars)))]
     {:db updated-db
      :fx [(when (= (count (:queue db)) 0)
             [:dispatch [:bg-player/fetch-stream (:url stream) 0 true]])
           (when notify?
             [:dispatch
              [:notifications/add
               {:status-text "Added stream to queue"}]])
           (when (and (get-in db [:settings :seamless-playback])
                      (or (= (count (:queue db)) 0)
                          (= (inc (:queue/position db)) (count (:queue db)))))
             [:dispatch
              [:queue/change-seamless-pos (dec (count (:queue db)))]])]})))

(rf/reg-event-fx
 :queue/add-n
 [persist]
 (fn [{:keys [db]} [_ streams notify? pos]]
   (let [updated-db
         (update
          db
          :queue
          #(into (into [] %1) (into [] %2))
          (->> streams
               (filter #(not (some #{(:type %)} ["playlist" "channel"])))
               (map #(-> %
                         (get-stream-metadata)
                         (utils/apply-image-quality db :thumbnail :thumbnails)
                         (utils/apply-image-quality db
                                                    :uploader-avatar
                                                    :uploader-avatars)))))]
     {:db updated-db
      :fx [(when (and (not pos) (= (count (:queue db)) 0))
             [:dispatch
              [:bg-player/fetch-stream
               (-> streams
                   first
                   :url)
               (count (:queue db)) (= (count (:queue db)) 0)]])
           (when (and (get-in db [:settings :seamless-playback])
                      (or pos
                          (= (count (:queue db)) 0)
                          (= (inc (:queue/position db))
                             (count (:queue db)))))
             [:dispatch
              [:queue/change-seamless-pos
               (cond
                 (= (count (:queue db)) 0) 0
                 pos                       pos
                 :else                     (dec (count (:queue db))))]])
           (when notify?
             [:dispatch
              [:notifications/add
               {:status-text (str "Added "
                                  (count streams)
                                  " streams to queue")}]])]})))

(rf/reg-event-fx
 :queue/remove
 [persist]
 (fn [{:keys [db]} [_ pos]]
   (let [updated-db   (update db
                              :queue
                              #(into (subvec % 0 pos) (subvec % (inc pos))))
         queue-pos    (:queue/position db)
         queue-length (count (:queue updated-db))]
     {:db updated-db
      :fx (cond
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
 :queue/load-pos
 (fn [{:keys [db]} [_ i]]
   (let [idx    (if (< i (count (:queue db)))
                  i
                  (when (= (:player/loop db) :playlist) 0))
         stream (get (:queue db) idx)]
     (when stream
       {:fx [[:dispatch [:bg-player/fetch-stream (:url stream) idx true]]
             (when (and (get-in db [:settings :seamless-playback])
                        (> (count (:queue db)) 1))
               [:dispatch [:queue/change-seamless-pos idx]])]}))))

(rf/reg-event-db
 :queue/load-thumbnail-color
 [persist]
 (fn [db [_ idx color]]
   (assoc-in db [:queue idx :thumbnail-color] (get (js->clj color) "hex"))))

(rf/reg-event-fx
 :queue/change-pos
 (fn [{:keys [db]} [_ i]]
   (let [idx    (cond (and (>= i 0) (< i (count (:queue db))))       i
                      (and (>= i 0) (= (:player/loop db) :playlist)) 0
                      (= (:player/loop db) :playlist)                (->
                                                                       db
                                                                       :queue
                                                                       count
                                                                       dec))
         stream (get (:queue db) idx)]
     (when stream
       {:fx (if (get-in db [:settings :seamless-playback])
              [[:dispatch
                [:queue/change-stream stream idx true]]
               [:dispatch [:queue/change-seamless-pos idx]]]
              [[:dispatch
                [:bg-player/fetch-stream (:url stream) idx
                 true]]])}))))

(rf/reg-event-fx
 :queue/reload-current-stream
 (fn [{:keys [db]} [_ _player]]
   {:fx [[:dispatch [:queue/load-pos (:queue/position db)]]]}))

(rf/reg-event-fx
 :queue/change-seamless-pos
 (fn [{:keys [db]} [_ idx]]
   (let [stream      (get (:queue db) idx)
         prev-idx    (when (> (count (:queue db)) 2)
                       (if (= idx 0)
                         (dec (count (:queue db)))
                         (dec idx)))
         next-idx    (when (> (count (:queue db)) 1)
                       (if (= (count (:queue db)) (inc idx))
                         0
                         (inc idx)))
         prev-stream (get (:queue db) prev-idx)
         next-stream (get (:queue db) next-idx)]
     (when stream
       {:fx [(when prev-stream
               [:dispatch
                [:bg-player/fetch-stream (:url prev-stream) prev-idx nil]])
             (when next-stream
               [:dispatch
                [:bg-player/fetch-stream (:url next-stream) next-idx
                 nil]])]}))))

(rf/reg-event-fx
 :queue/change-stream
 [persist (rf/inject-cofx ::inject/sub [:bg-player])
  (rf/inject-cofx ::inject/sub [:queue-bg])
  (rf/inject-cofx ::inject/sub [:queue-thumbnail])
  (rf/inject-cofx ::inject/sub [:dark-theme])]
 (fn [{:keys [db bg-player queue-bg queue-thumbnail dark-theme]}
      [_ stream idx play?]]
   (when stream
     (let [updated-db (update-in
                       db
                       [:queue idx]
                       #(merge
                         %
                         (->
                           stream
                           (utils/apply-image-quality db :thumbnail :thumbnails)
                           (utils/apply-image-quality db
                                                      :uploader-avatar
                                                      :uploader-avatars)
                           (utils/apply-thumbnails-quality
                            db
                            :related-streams)
                           (utils/apply-avatars-quality
                            db
                            :related-streams))))]
       {:db (if play? (assoc updated-db :queue/position idx) updated-db)
        :fx (into (if play?
                    [[:dispatch
                      [:get-color-async
                       (get-in updated-db [:queue idx :thumbnail])
                       [:queue/load-thumbnail-color idx]]]
                     [:dispatch
                      [:animate @queue-bg
                       {"--opacity" [(if dark-theme 0.8 0.3) 0.5]}
                       {:duration 0.5 :ease "easeIn"}]]
                     [:dispatch
                      [:animate @queue-thumbnail {:scale [0.9 1]}
                       {:type "spring" :ease "easeInOut"}]]
                     [:media-session-metadata
                      {:title   (:name stream)
                       :artist  (:uploader-name stream)
                       :artwork [{:src (-> stream
                                           :thumbnails
                                           last
                                           :url)}]}]
                     [:media-session-handlers
                      {:current-pos idx
                       :player      bg-player}]
                     [:dispatch
                      [(if (:main-player/show db)
                         :main-player/set-stream
                         :bg-player/set-stream) stream idx]]
                     [:dispatch [:scroll-to-index idx]]]
                    [])
                  (when (and (:main-player/show db)
                             (not (seq (get-in db
                                               [:queue idx :comments-page]))))
                    [[:dispatch
                      [:comments/fetch-page (:url stream) [:queue idx]]]]))}))))

(rf/reg-event-fx
 :queue/scroll-to-pos
 (fn [{:keys [db]} _]
   {:timeout
    {:id    (nano-id)
     :event [:scroll-to-index (:queue/position db)]
     :time  100}}))
