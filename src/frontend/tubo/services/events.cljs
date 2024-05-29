(ns tubo.services.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-fx
 :services/fetch
 (fn [{:keys [db]} [_ {:keys [service-id]}]]
   {:db db
    :fx [[:dispatch [:services/change-id service-id]]
         [:dispatch [:kiosks/fetch-all service-id
                     [:kiosks/load] [:bad-response]]]]}))

(rf/reg-event-fx
 :services/change-id
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ service-id]]
   {:db    (assoc db :service-id service-id)
    :store (assoc store :service-id service-id)}))

(rf/reg-event-fx
 :services/fetch-all
 (fn [{:keys [db]} [_ on-success on-error]]
   (api/get-request "/services" on-success on-error)))

(rf/reg-event-db
 :services/load
 (fn [db [_ res]]
   (assoc db :services (js->clj res :keywordize-keys true))))
