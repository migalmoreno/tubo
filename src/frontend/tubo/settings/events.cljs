(ns tubo.settings.events
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [re-frame.core :as rf]
   [tubo.interceptors :refer [persist]]
   [tubo.schemas :refer [Settings]]))

(rf/reg-event-fx
 :settings/change
 [persist]
 (fn [{:keys [db]} [_ keys val]]
   (let [update-settings #(assoc-in % (into [:settings] keys) val)]
     {:db (update-settings db)})))

(rf/reg-event-fx
 :settings/fetch-page
 (fn []
   {:document-title "Settings"}))

(defn normalize-config
  [config]
  (-> (reduce-kv
       (fn [acc k v]
         (assoc acc
                k
                (if (#{:default-country :default-kiosk :default-filter} k)
                  (update-keys v #(js/parseInt (name %)))
                  v)))
       {}
       config)
      (#(m/decode Settings % (mt/strip-extra-keys-transformer)))))

(rf/reg-event-fx
 :settings/load
 (fn [{:keys [db]} [_ on-success {:keys [body]}]]
   {:db (assoc db :settings (merge (normalize-config body) (:settings db)))
    :fx [[:dispatch on-success]]}))

(rf/reg-event-fx
 :settings/fetch
 (fn [_ [_ on-success]]
   {:fx [[:dispatch
          [:api/get "config" [:settings/load on-success] [:bad-response]]]]}))
