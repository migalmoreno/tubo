(ns tubo.services.events
  (:require
   [fork.re-frame :as fork]
   [re-frame.core :as rf]
   [tubo.storage :refer [persist]]))

(rf/reg-event-fx
 :services/fetch
 (fn [{:keys [db]} [_ {:keys [service-id]}]]
   {:db db
    :fx [[:dispatch [:services/change-id service-id]]
         [:dispatch
          [:kiosks/fetch-all service-id
           [:kiosks/load] [:bad-response]]]]}))

(rf/reg-event-fx
 :services/change-id
 [persist]
 (fn [{:keys [db]} [_ service-id]]
   {:db (assoc db :service-id service-id)}))

(rf/reg-event-fx
 :services/fetch-all
 (fn [_ [_ on-success on-error]]
   {:fx [[:dispatch [:api/get "services" on-success on-error]]]}))

(rf/reg-event-db
 :services/load
 (fn [db [_ {:keys [body]}]]
   (assoc db :services body)))

(rf/reg-event-fx
 :peertube/delete-instance
 [persist]
 (fn [{:keys [db]} [_ instance]]
   (let [pos        (.indexOf (:peertube/instances db) instance)
         updated-db (update db
                            :peertube/instances
                            #(into (subvec % 0 pos) (subvec % (inc pos))))]
     {:db updated-db})))

(rf/reg-event-fx
 :peertube/add-instance
 [persist]
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   (if (some #(= (:url %) (:url body)) (:peertube/instances db))
     {:fx [[:dispatch [:notifications/error "Instance already exists"]]]
      :db (fork/set-submitting db path false)}
     {:db (-> (fork/set-submitting db path false)
              (update :peertube/instances conj body))
      :fx [[:dispatch [:modals/close]]]})))

(rf/reg-event-fx
 :peertube/load-instances
 (fn [_ [_ instance {:keys [body]}]]
   {:fx [[:dispatch [:peertube/load-active-instance {:body instance}]]
         [:dispatch [:notifications/success body]]]}))

(rf/reg-event-fx
 :peertube/load-active-instance
 [persist]
 (fn [{:keys [db]} [_ {:keys [body]}]]
   (let [updated-db (update db
                            :peertube/instances
                            #(into []
                                   (map (fn [it]
                                          (if (= (:url it) (:url body))
                                            (assoc it :active? true)
                                            (assoc it :active? false)))
                                        (if (some (fn [it]
                                                    (= (:url it) (:url body)))
                                                  (:peertube/instances db))
                                          %
                                          (conj % body)))))]
     {:db updated-db})))

(rf/reg-event-fx
 :peertube/change-instance
 (fn [_ [_ instance]]
   {:fx [[:dispatch
          [:api/post "services/3/change-instance" instance
           [:peertube/load-instances instance]
           [:bad-response]]]]}))

(rf/reg-event-fx
 :peertube/create-instance
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/get
           (str "services/3/instance-metadata/"
                (js/encodeURIComponent (:url values)))
           [:peertube/add-instance path]
           [:on-form-submit-failure path]]]]}))
