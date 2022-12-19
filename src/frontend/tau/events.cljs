(ns tau.events
  (:require
   [re-frame.core :as rf]
   [tau.api :as api]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:global-search ""
    :service-id 0
    :stream {}
    :search-results []
    :services []
    :current-match nil}))

(rf/reg-event-db
 :navigated
 (fn [db [_ new-match]]
   (assoc db :current-match new-match)))

(rf/reg-event-db
 :bad-response
 (fn [db [_ res]]
   (assoc db :http-response (get-in res [:response :error]))))

(rf/reg-event-db
 :change-global-search
 (fn [db [_ res]]
   (assoc db :global-search res)))

(rf/reg-event-db
 :change-service-id
 (fn [db [_ res]]
   (assoc db :service-id res)))

(rf/reg-event-fx
 :switch-to-global-player
 (fn [{:keys [db]} [_ res]]
   {:db (assoc db :show-global-player true)
    :dispatch [:change-global-search res]}))

(rf/reg-event-db
 :load-services
 (fn [db [_ res]]
   (assoc db :services (js->clj res :keywordize-keys true)
          :show-loading false)))

(rf/reg-event-fx
 :get-services
 (fn [{:keys [db]} _]
   (assoc
    (api/get-request "/api/services" [:load-services] [:bad-response])
    :db (assoc db :show-loading true))))

(rf/reg-event-db
 :load-stream
 (fn [db [_ res]]
   (assoc db :stream (js->clj res :keywordize-keys true)
          :show-loading false)))

(rf/reg-event-fx
 :get-stream
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request "/api/stream" [:load-stream] [:bad-response] {:url uri})
    :db (assoc db :show-loading true))))

(rf/reg-event-db
 :load-search-results
 (fn [db [_ res]]
   (assoc db :search-results (js->clj res :keywordize-keys true)
          :show-loading false)))

(rf/reg-event-fx
 :get-search-results
 (fn [{:keys [db]} [_ {:keys [id query]}]]
   (assoc
    (api/get-request "/api/search"
                     [:load-search-results] [:bad-response]
                     {:serviceId id :q query})
    :db (assoc db :show-loading true))))
