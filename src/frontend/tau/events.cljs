(ns tau.events
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.api :as api]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   {:global-search ""
    :service-id 0
    :service-color "#cc0000"
    :stream {}
    :search-results []
    :services []
    :current-match nil
    :page-scroll 0}))

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
 ::page-scroll
 (fn [db _]
   (when (> (.-scrollY js/window) 0)
     (assoc db :page-scroll (+ (.-scrollY js/window) (.-innerHeight js/window))))))

(rf/reg-event-db
 ::reset-page-scroll
 (fn [db _]
   (assoc db :page-scroll 0)))

(rf/reg-event-fx
 ::navigated
 (fn [{:keys [db]} [_ new-match]]
   {:db (-> db
            (assoc :current-match new-match)
            (assoc :show-pagination-loading false))
    ::scroll-to-top nil}))

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
   (assoc db :http-response (get-in res [:response :error]))))

(rf/reg-event-db
 ::change-global-search
 (fn [db [_ res]]
   (assoc db :global-search res)))

(rf/reg-event-db
 ::change-service-color
 (fn [db [_ service-id]]
   (assoc db :service-color
          (case service-id
            0 "#cc0000"
            1 "#ff7700"
            2 "#333333"
            3 "#F2690D"
            4 "#629aa9"))))

(rf/reg-event-fx
 ::change-service-id
 (fn [{:keys [db]} [_ service-id]]
   {:db (assoc db :service-id service-id)
    :fx [[:dispatch [::change-service-color service-id]]]}))

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
 ::switch-to-global-player
 (fn [{:keys [db]} [_ res]]
   {:db (assoc db :show-global-player true)
    :fx [[:dispatch [::change-global-search res]]]}))

(rf/reg-event-db
 ::load-services
 (fn [db [_ res]]
   (assoc db :services (js->clj res :keywordize-keys true))))

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
 ::toggle-comments
 (fn [db [_ res]]
   (assoc-in db [:stream :show-comments] (not (-> db :stream :show-comments)))))

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
   (api/get-request (str "/api/services/" id "/kiosks") [::load-kiosks] [::bad-response])))

(rf/reg-event-db
 ::load-kiosk
 (fn [db [_ res]]
   (assoc db :kiosk (js->clj res :keywordize-keys true)
          :show-page-loading false)))

(rf/reg-event-fx
 ::get-default-kiosk
 (fn [{:keys [db]} [_ service-id]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/default-kiosk")
                     [::load-kiosk] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::get-kiosk
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   (if kiosk-id
     (assoc
      (api/get-request (str "/api/services/" service-id "/kiosks/"
                            (js/encodeURIComponent kiosk-id))
                       [::load-kiosk] [::bad-response])
      :db (assoc db :show-page-loading true))
     {:fx [[:dispatch [::get-default-kiosk service-id]]]})))

(rf/reg-event-fx
 ::change-service
 (fn [{:keys [db]} [_ service-id]]
   {:fx [[:dispatch
          [::navigate {:name :tau.routes/kiosk
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
       (str "/api/services/" service-id "/kiosks/" (js/encodeURIComponent kiosk-id))
       [::load-paginated-kiosk-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-stream
 (fn [db [_ res]]
   (assoc db :stream (js->clj res :keywordize-keys true)
          :show-page-loading false)))

(rf/reg-event-fx
 ::get-stream
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/api/streams/" (js/encodeURIComponent uri))
                     [::load-stream] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-db
 ::load-channel
 (fn [db [_ res]]
   (assoc db :channel (js->clj res :keywordize-keys true)
          :show-page-loading false)))

(rf/reg-event-fx
 ::get-channel
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request
     (str "/api/channels/" (js/encodeURIComponent uri))
     [::load-channel] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-db
 ::load-playlist
 (fn [db [_ res]]
   (assoc db :playlist (js->clj res :keywordize-keys true)
          :show-page-loading false)))

(rf/reg-event-fx
 ::get-playlist
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/api/playlists/" (js/encodeURIComponent uri))
                     [::load-playlist] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-db
 ::load-search-results
 (fn [db [_ res]]
   (assoc db :search-results (js->clj res :keywordize-keys true)
          :show-page-loading false
          :global-search "")))

(rf/reg-event-fx
 ::get-search-results
 (fn [{:keys [db]} [_ service-id query]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/search")
                     [::load-search-results] [::bad-response]
                     {:q query})
    :db (assoc db :show-page-loading true))))
