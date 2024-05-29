(ns tubo.notifications.events
  (:require
   [re-frame.core :as rf]
   [nano-id.core :refer [nano-id]]))

(rf/reg-event-fx
 :notifications/add
 (fn [{:keys [db]} [_ data time]]
   (let [id (nano-id)
         updated-db (update db :notifications #(into [] (conj %1 %2))
                            (assoc data :id id))]
     {:db updated-db
      :fx (if (false? time)
              []
              [[:timeout {:id    id
                          :event [:notifications/remove id]
                          :time  (or time 2000)}]])})))

(rf/reg-event-db
 :notifications/remove
 (fn [db [_ id]]
   (update db :notifications #(remove (fn [notification]
                                        (= (:id notification) id)) %))))

(rf/reg-event-db
 :notifications/clear
 (fn [db _]
   (dissoc db :notifications)))
