(ns tubo.kiosks.events
  (:require
   [re-frame.core :as rf]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.utils :as utils]))

(rf/reg-event-db
 :kiosks/load
 (fn [db [_ {:keys [body]}]]
   (assoc db :kiosks body)))

(rf/reg-event-fx
 :kiosks/fetch
 [(show-loading-status :api/get)]
 (fn [_ [_ service-id kiosk-id on-success on-error params]]
   {:fx [[:dispatch
          [:api/get
           (str "services/" service-id
                "/kiosks/"  (js/encodeURIComponent kiosk-id))
           on-success on-error params]]]}))

(rf/reg-event-fx
 :kiosks/fetch-default
 [(show-loading-status :api/get)]
 (fn [_ [_ service-id on-success on-error params]]
   {:fx [[:dispatch
          [:api/get
           (str "services/" service-id "/default-kiosk")
           on-success on-error params]]]}))

(rf/reg-event-fx
 :kiosks/fetch-all
 (fn [_ [_ id on-success on-error]]
   {:fx [[:dispatch
          [:api/get
           (str "services/" id "/kiosks")
           on-success on-error]]]}))

(rf/reg-event-fx
 :kiosks/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :kiosk
               (-> body
                   (utils/apply-thumbnails-quality db :related-streams)
                   (utils/apply-avatars-quality db :related-streams)))
    :fx [[:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :kiosks/fetch-page
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   (let [default-country (-> db
                             :settings
                             :default-country
                             (get (js/parseInt service-id))
                             :code)]
     {:fx [[:dispatch
            (if kiosk-id
              [:kiosks/fetch service-id kiosk-id
               [:kiosks/load-page]
               [:bad-page-response [:kiosks/fetch-page service-id kiosk-id]]
               (when default-country {:region default-country})]
              [:kiosks/fetch-default-page service-id])]]})))

(rf/reg-event-fx
 :kiosks/fetch-default-page
 (fn [{:keys [db]} [_ service-id]]
   (let [default-kiosk-id (-> db
                              :settings
                              :default-kiosk
                              (get (js/parseInt service-id)))
         default-country  (-> db
                              :settings
                              :default-country
                              (get (js/parseInt service-id))
                              :code)]
     {:fx [[:dispatch
            (if default-kiosk-id
              [:kiosks/fetch-page service-id default-kiosk-id]
              [:kiosks/fetch-default service-id
               [:kiosks/load-page]
               [:bad-page-response [:kiosks/fetch-default-page service-id]]
               (when default-country {:region default-country})])]]})))

(rf/reg-event-fx
 :kiosks/change-page
 (fn [_ [_ service-id]]
   {:fx [[:dispatch [:services/change-id service-id]]
         [:dispatch
          [:navigation/navigate
           {:name   :kiosk-page
            :params {}
            :query  {:serviceId service-id}}]]]}))

(rf/reg-event-db
 :kiosks/load-paginated
 (fn [db [_ {:keys [body]}]]
   (->
     db
     (update-in [:kiosk :related-streams]
                #(into %1 %2)
                (-> body
                    (utils/apply-thumbnails-quality db :related-streams)
                    (utils/apply-avatars-quality db :related-streams)
                    :related-streams))
     (assoc-in [:kiosk :next-page] (:next-page body))
     (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :kiosks/fetch-paginated
 (fn [{:keys [db]} [_ service-id kiosk-id next-page]]
   (if (seq next-page)
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch
            [:api/get
             (str "services/" service-id
                  "/kiosks/"  (js/encodeURIComponent kiosk-id))
             [:kiosks/load-paginated] [:bad-pagination-response]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]}
     {:db (assoc db :show-pagination-loading false)})))
