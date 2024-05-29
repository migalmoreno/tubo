(ns tubo.kiosks.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-db
 :kiosks/load
 (fn [db [_ res]]
   (assoc db :kiosks (js->clj res :keywordize-keys true))))

(rf/reg-event-fx
 :kiosks/fetch
 (fn [{:keys [db]} [_ service-id kiosk-id on-success on-error params]]
   (api/get-request (str "/services/" service-id "/kiosks/"
                         (js/encodeURIComponent kiosk-id))
                    on-success on-error params)))

(rf/reg-event-fx
 :kiosks/fetch-default
 (fn [{:keys [db]} [_ service-id on-success on-error]]
   (api/get-request (str "/services/" service-id "/default-kiosk")
                    on-success on-error)))

(rf/reg-event-fx
 :kiosks/fetch-all
 (fn [{:keys [db]} [_ id on-success on-error]]
   (api/get-request (str "/services/" id "/kiosks")
                    on-success on-error)))

(rf/reg-event-fx
 :kiosks/load-page
 (fn [{:keys [db]} [_ res]]
   (let [kiosk-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :kiosk kiosk-res
                 :show-page-loading false)
      :fx [[:dispatch [:services/fetch kiosk-res]]
           [:document-title (:id kiosk-res)]]})))

(rf/reg-event-fx
 :kiosks/fetch-page
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   {:db (assoc db :show-page-loading true)
    :fx [[:dispatch (if kiosk-id
                      [:kiosks/fetch service-id kiosk-id
                       [:kiosks/load-page] [:bad-response]]
                      [:kiosks/fetch-default-page service-id])]]}))

(rf/reg-event-fx
 :kiosks/fetch-default-page
 (fn [{:keys [db]} [_ service-id]]
   (let [default-kiosk-id
         (when (= (js/parseInt service-id)
                  (-> db :settings :default-service :service-id))
           (-> db :settings :default-service :default-kiosk))]
     {:fx [[:dispatch (if default-kiosk-id
                        [:kiosks/fetch-page service-id default-kiosk-id]
                        [:kiosks/fetch-default service-id
                         [:kiosks/load-page] [:bad-response]])]]})))

(rf/reg-event-fx
 :kiosks/change-page
 (fn [{:keys [db]} [_ service-id]]
   {:fx [[:dispatch [:services/change-id service-id]]
         [:dispatch
          [:navigate {:name   :kiosk-page
                      :params {}
                      :query  {:serviceId service-id}}]]]}))

(rf/reg-event-db
 :kiosks/load-paginated
 (fn [db [_ res]]
   (-> db
       (update-in [:kiosk :related-streams] #(into %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:kiosk :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :kiosks/fetch-paginated
 (fn [{:keys [db]} [_ service-id kiosk-id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch [:kiosks/fetch service-id kiosk-id
                       [:kiosks/load-paginated] [:bad-response]
                       {:nextPage (js/encodeURIComponent next-page-url)}]]]})))
