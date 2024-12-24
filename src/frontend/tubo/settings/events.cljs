(ns tubo.settings.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :settings/change
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ keys val]]
   (let [update-settings #(assoc-in % (into [:settings] keys) val)]
     {:db    (update-settings db)
      :store (update-settings store)})))

(rf/reg-event-fx
 :settings/fetch-page
 (fn []
   {:document-title "Settings"}))
