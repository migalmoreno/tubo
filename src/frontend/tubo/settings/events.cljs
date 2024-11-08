(ns tubo.settings.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-fx
 :settings/change
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ key val]]
   {:db    (assoc-in db [:settings key] val)
    :store (assoc store key val)}))

(rf/reg-event-fx
 :settings/load-kiosks
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ service-name service-id res]]
   (let [kiosks-res            (js->clj res :keywordize-keys true)
         default-service-kiosk (-> db
                                   :settings
                                   :default-service
                                   :default-kiosk)
         default-kiosk         (if (some #(= % default-service-kiosk)
                                         (:available-kiosks kiosks-res))
                                 default-service-kiosk
                                 (:default-kiosk kiosks-res))]
     {:db    (update-in db
                        [:settings :default-service]
                        assoc
                        :id               service-name
                        :service-id       service-id
                        :available-kiosks (:available-kiosks kiosks-res)
                        :default-kiosk    default-kiosk)
      :store (update-in store
                        [:default-service]
                        assoc
                        :id               service-name
                        :service-id       service-id
                        :available-kiosks (:available-kiosks kiosks-res)
                        :default-kiosk    default-kiosk)})))

(rf/reg-event-fx
 :settings/change-service
 [(rf/inject-cofx :store)]
 (fn [{:keys [db]} [_ val]]
   (let [service-id (-> (filter #(= val
                                    (-> %
                                        :info
                                        :name))
                                (:services db))
                        first
                        :id)]
     (api/get-request (str "/services/" service-id "/kiosks")
                      [:settings/load-kiosks val service-id]
                      [:bad-response]))))

(rf/reg-event-fx
 :settings/change-kiosk
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ val]]
   {:db    (assoc-in db [:settings :default-service :default-kiosk] val)
    :store (assoc-in store [:default-service :default-kiosk] val)}))

(rf/reg-event-fx
 :settings/fetch-page
 (fn [{:keys [db]} _]
   (let [id         (-> db
                        :settings
                        :default-service
                        :id)
         service-id (-> db
                        :settings
                        :default-service
                        :service-id)]
     (assoc
      (api/get-request (str "/services/" service-id "/kiosks")
                       [:settings/load-kiosks id service-id]
                       [:bad-response])
      :document-title
      "Settings"))))
