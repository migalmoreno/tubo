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

(rf/reg-event-fx
 ::navigated
 (fn [{:keys [db]} [_ new-match]]
   {::scroll-to-top nil
    :db (assoc db :current-match new-match)
    :fx [[:dispatch [::reset-page-scroll]]]}))

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
 (fn [db [_ id]]
   (assoc db :service-color
          (case id
            0 "#cc0000"
            1 "#ff7700"
            2 "#333333"
            3 "#F2690D"
            4 "#629aa9"))))

(rf/reg-event-fx
 ::change-service-id
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :service-id id)
    :fx [[:dispatch [::change-service-color id]]]}))

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
 ::scroll-channel-pagination
 (fn [{:keys [db]} [_ uri next-page-url]]
   (assoc
    (api/get-request
     (str "/api/channels/" (js/encodeURIComponent uri) )
     [::load-paginated-channel-results] [::bad-response]
     {:nextPage (js/encodeURIComponent next-page-url)})
    :db (assoc db :show-pagination-loading true))))

(rf/reg-event-db
 ::load-paginated-search-results
 (fn [db [_ res]]
   (-> db
       (update-in [:search-results :items] #(apply conj %1 %2)
                  (:items (js->clj res :keywordize-keys true)))
       (assoc-in [:search-results :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-db
 ::reset-page-scroll
 (fn [db _]
   (assoc db :page-scroll 0)))

(rf/reg-event-db
 ::page-scroll
 (fn [db _]
   (assoc db :page-scroll (+ (.-scrollY js/window) (.-innerHeight js/window)))))

(rf/reg-event-fx
 ::scroll-search-pagination
 (fn [{:keys [db]} [_ query id next-page-url]]
   (assoc
    (api/get-request
     (str "/api/services/" id "/search")
     [::load-paginated-search-results] [::bad-response]
     {:q query
      :nextPage (js/encodeURIComponent next-page-url)})
    :db (assoc db :show-pagination-loading true))))

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
 ::get-kiosk
 (fn [{:keys [db]} [_ {:keys [service-id kiosk-id]}]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/kiosks/"
                          (js/encodeURIComponent kiosk-id))
                     [::load-kiosk] [::bad-response])
    :db (assoc db :show-page-loading true))))

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
          :show-page-loading false)))

(rf/reg-event-fx
 ::get-search-results
 (fn [{:keys [db]} [_ {:keys [service-id query]}]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/search")
                     [::load-search-results] [::bad-response]
                     {:q query})
    :db (assoc db :show-page-loading true))))
