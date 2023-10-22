(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [tubo.api :as api]))

(reg-co-fx! :tubo {:fx :store :cofx :store})

(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx :store)]
  (fn [{:keys [store]} _]
    (let [{:keys [current-theme show-comments show-related show-description
                  media-queue media-queue-pos show-audio-player
                  loop-file loop-playlist]} store]
      {:db
       {:search-query   ""
        :service-id      0
        :stream          {}
        :search-results  []
        :services        []
        :loop-file (if (nil? loop-file) false loop-file)
        :loop-playlist (if (nil? loop-playlist) true loop-playlist)
        :media-queue     (if (nil? media-queue) [] media-queue)
        :media-queue-pos (if (nil? media-queue-pos) 0 media-queue-pos)
        :current-match   nil
        :show-audio-player (if (nil? show-audio-player) false show-audio-player)
        :settings
        {:current-theme (if (nil? current-theme) :light current-theme)
         :themes        #{:light :dark}
         :show-comments (if (nil? show-comments) true show-comments)
         :show-related  (if (nil? show-related) true show-related)
         :show-description (if (nil? show-description) true show-description)}}})))

(rf/reg-fx
 ::scroll-to-top
 (fn [_]
   (.scrollTo js/window #js {"top" 0 "behavior" "smooth"})))

(rf/reg-fx
 ::history-back!
 (fn [_]
   (.back js/window.history)))

(rf/reg-event-fx
 ::history-back
 (fn [_ _]
   {::history-back! nil}))

(rf/reg-event-db
 ::toggle-mobile-nav
 (fn [db _]
   (assoc db :show-mobile-nav (not (:show-mobile-nav db)))))

(rf/reg-event-fx
 ::toggle-loop-file
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   {:db (assoc db :loop-file (not (:loop-file db)))
    :store (assoc store :loop-file (not (:loop-file store)))}))

(rf/reg-event-fx
 ::toggle-loop-playlist
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   {:db (assoc db :loop-playlist (not (:loop-playlist db)))
    :store (assoc store :loop-playlist (not (:loop-playlist store)))}))

(rf/reg-event-fx
 ::navigated
 (fn [{:keys [db]} [_ new-match]]
   (let [old-match (:current-match db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)
         match (assoc new-match :controllers controllers)]
     {:db (-> db
              (assoc :current-match match)
              (assoc :show-pagination-loading false))
      ::scroll-to-top nil})))

(rf/reg-event-fx
 ::navigate
 (fn [_ [_ route]]
   {::navigate! route}))

(rf/reg-fx
 ::navigate!
 (fn [{:keys [name params query]}]
   (rfe/push-state name params query)))

(rf/reg-event-db
 ::bad-response
 (fn [db [_ res]]
   (.log js/console (clj->js res))
   (assoc db :http-response (get-in res [:response :error]))))

(rf/reg-event-db
 ::change-search-query
 (fn [db [_ res]]
   (assoc db :search-query res)))

(rf/reg-event-db
 ::change-service-id
 (fn [db [_ service-id]]
   (assoc db :service-id service-id)))

(rf/reg-event-db
 ::load-paginated-channel-results
 (fn [db [_ res]]
   (-> db
       (update-in [:channel :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:channel :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::channel-pagination
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/api/channels/" (js/encodeURIComponent uri) )
       [::load-paginated-channel-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-paginated-playlist-results
 (fn [db [_ res]]
   (-> db
       (update-in [:playlist :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:playlist :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::playlist-pagination
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/api/playlists/" (js/encodeURIComponent uri))
       [::load-paginated-playlist-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-paginated-search-results
 (fn [db [_ res]]
   (-> db
       (update-in [:search-results :items] #(apply conj %1 %2)
                  (:items (js->clj res :keywordize-keys true)))
       (assoc-in [:search-results :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::search-pagination
 (fn [{:keys [db]} [_ query id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/api/services/" id "/search")
       [::load-paginated-search-results] [::bad-response]
       {:q query
        :nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-fx
 ::add-to-media-queue
  [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream]]
   (let [updated-db (update db :media-queue conj stream)]
     {:db updated-db
      :store (assoc store :media-queue (:media-queue updated-db))
      :fx [[:dispatch [::fetch-audio-player-stream (:url stream)]]]})))

(rf/reg-event-fx
 ::change-media-queue-pos
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ idx]]
   {:db (assoc db :media-queue-pos idx)
    :store (assoc store :media-queue-pos idx)}))

(rf/reg-event-fx
 ::change-media-queue-stream
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ src]]
   (let [idx (- (count (:media-queue db)) 1)
         update-entry
         (fn [elem] (assoc-in elem [:media-queue idx :stream] src))]
     (when-not (-> db :media-queue (nth idx) :stream)
       {:db (update-entry db)
        :store (update-entry store)}))))

(rf/reg-event-fx
 ::toggle-audio-player
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [remove-entries
         (fn [elem]
           (-> elem
               (assoc :show-audio-player (not (:show-audio-player elem)))
               (assoc :media-queue [])
               (assoc :media-queue-pos 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)})))

(rf/reg-event-fx
 ::switch-to-audio-player
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream]]
   {:db (assoc db :show-audio-player true)
    :store (assoc store :show-audio-player true)
    :fx [[:dispatch [::add-to-media-queue stream]]]}))

(rf/reg-event-db
 ::load-services
 (fn [db [_ res]]
   (assoc db :services (js->clj res :keywordize-keys true))))

(rf/reg-event-fx
 ::set-service-styles
 (fn [{:keys [db]} [_ res]]
   {:db db
    :fx [[:dispatch [::change-service-id (:service-id res)]]
         [:dispatch [::get-kiosks (:service-id res)]]]}))

(rf/reg-event-fx
 ::get-services
 (fn [{:keys [db]} _]
   (api/get-request "/api/services" [::load-services] [::bad-response])))

(rf/reg-event-db
 ::load-comments
 (fn [db [_ res]]
   (-> db
       (assoc-in [:stream :comments-page] (js->clj res :keywordize-keys true))
       (assoc-in [:stream :show-comments-loading] false))))

(rf/reg-event-fx
 ::get-comments
 (fn [{:keys [db]} [_ url]]
   (assoc
    (api/get-request (str "/api/comments/" (js/encodeURIComponent url))
                     [::load-comments] [::bad-response])
    :db (-> db
            (assoc-in [:stream :show-comments-loading] true)
            (assoc-in [:stream :show-comments] true)))))

(rf/reg-event-db
 ::toggle-stream-layout
 (fn [db [_ layout]]
   (assoc-in db [:stream layout] (not (-> db :stream layout)))))

(rf/reg-event-db
 ::toggle-comment-replies
 (fn [db [_ comment-id]]
   (update-in db [:stream :comments-page :comments]
              (fn [comments]
                (map #(if (= (:id %) comment-id)
                        (assoc % :show-replies (not (:show-replies %)))
                        %)
                     comments)))))

(rf/reg-event-db
 ::load-paginated-comments
 (fn [db [_ res]]
   (-> db
       (update-in [:stream :comments-page :comments] #(apply conj %1 %2)
                  (:comments (js->clj res :keywordize-keys true)))
       (assoc-in [:stream :comments-page :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::comments-pagination
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request (str "/api/comments/" (js/encodeURIComponent url))
                       [::load-paginated-comments] [::bad-response]
                       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-kiosks
 (fn [db [_ res]]
   (assoc db :kiosks (js->clj res :keywordize-keys true))))

(rf/reg-event-fx
 ::get-kiosks
 (fn [{:keys [db]} [_ id]]
   (api/get-request (str "/api/services/" id "/kiosks")
                    [::load-kiosks] [::bad-response])))

(rf/reg-event-fx
 ::load-kiosk
 (fn [{:keys [db]} [_ res]]
   (let [kiosk-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :kiosk kiosk-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles kiosk-res]]]})))

(rf/reg-event-fx
 ::get-default-kiosk-page
 (fn [{:keys [db]} [_ service-id]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/default-kiosk")
                     [::load-kiosk] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::get-kiosk-page
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   (if kiosk-id
     (assoc
      (api/get-request (str "/api/services/" service-id "/kiosks/"
                            (js/encodeURIComponent kiosk-id))
                       [::load-kiosk] [::bad-response])
      :db (assoc db :show-page-loading true))
     {:fx [[:dispatch [::get-default-kiosk-page service-id]]]})))

(rf/reg-event-fx
 ::change-service-kiosk
 (fn [{:keys [db]} [_ service-id]]
   {:fx [[:dispatch [::change-service-id service-id]]
         [:dispatch
          [::navigate {:name :tubo.routes/kiosk
                       :params {}
                       :query  {:serviceId service-id}}]]]}))

(rf/reg-event-db
 ::load-paginated-kiosk-results
 (fn [db [_ res]]
   (-> db
       (update-in [:kiosk :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:kiosk :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::kiosk-pagination
 (fn [{:keys [db]} [_ service-id kiosk-id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/api/services/" service-id "/kiosks/"
            (js/encodeURIComponent kiosk-id))
       [::load-paginated-kiosk-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-fx
 ::load-audio-player-stream
 (fn [{:keys [db]} [_ res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :show-audio-player-loading false)
      :fx [[:dispatch [::change-media-queue-stream
                       (-> stream-res :audio-streams first :content)]]]})))

(rf/reg-event-fx
 ::load-stream-page
 (fn [{:keys [db]} [_ res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :stream stream-res
                 :show-page-loading false)
      :fx [[:dispatch [::change-stream-format nil]]
           (when (and (-> db :settings :show-comments))
             [:dispatch [::get-comments (:url stream-res)]])
           [:dispatch [::set-service-styles stream-res]]]})))

(rf/reg-event-fx
 ::fetch-stream-page
 (fn [{:keys [db]} [_ uri]]
    (api/get-request (str "/api/streams/" (js/encodeURIComponent uri))
                     [::load-stream-page] [::bad-response])))

(rf/reg-event-fx
 ::fetch-audio-player-stream
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/api/streams/" (js/encodeURIComponent uri))
                     [::load-audio-player-stream] [::bad-response])
    :db (assoc db :show-audio-player-loading true))))

(rf/reg-event-fx
 ::get-stream-page
 (fn [{:keys [db]} [_ uri]]
   {:db (assoc db :show-page-loading true)
    :fx [[:dispatch [::fetch-stream-page uri]]]}))

(rf/reg-event-db
 ::change-stream-format
 (fn [{:keys [stream] :as db} [_ format-id]]
   (let [{:keys [audio-streams video-streams]} stream]
     (if format-id
       (assoc db :stream-format
              (first (filter #(= format-id (:id %))
                             (apply conj audio-streams video-streams))))
       (assoc db :stream-format (if (empty? video-streams)
                                  (first audio-streams)
                                  (last video-streams)))))))

(rf/reg-event-fx
 ::load-channel
 (fn [{:keys [db]} [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :channel channel-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles channel-res]]]})))

(rf/reg-event-fx
 ::get-channel-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request
     (str "/api/channels/" (js/encodeURIComponent uri))
     [::load-channel] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-playlist
 (fn [{:keys [db]} [_ res]]
   (let [playlist-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :playlist playlist-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles playlist-res]]]})))

(rf/reg-event-fx
 ::get-playlist-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/api/playlists/" (js/encodeURIComponent uri))
                     [::load-playlist] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-search-results
 (fn [{:keys [db]} [_ res]]
   (let [search-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :search-results search-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles search-res]]]})))

(rf/reg-event-fx
 ::get-search-page
 (fn [{:keys [db]} [_ service-id query]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/search")
                     [::load-search-results] [::bad-response]
                     {:q query})
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
  ::change-setting
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ key val]]
   {:db (assoc-in db [:settings key] val)
    :store (assoc store key val)}))
