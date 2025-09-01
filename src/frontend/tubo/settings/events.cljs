(ns tubo.settings.events
  (:require
   [re-frame.core :as rf]
   [tubo.storage :refer [persist]]))

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
