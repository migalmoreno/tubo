(ns tubo.modals.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]))

(rf/reg-event-db
 :modals/add
 (fn [db [_ data]]
   (update db :modals #(into [] (conj (into [] %1) %2)) data)))

(rf/reg-event-db
 :modals/delete
 (fn [db [_ id]]
   (update db :modals #(remove (fn [modal] (= (:id modal) id)) %))))

(rf/reg-event-fx
 :modals/hide
 (fn [{:keys [db]} _]
   {:db            (update db
                           :modals
                           #(map-indexed
                             (fn [i modal]
                               (when (= i (- (count %) 1))
                                 (assoc modal :show? false :child nil)))
                             %))
    :body-overflow false}))

(rf/reg-event-fx
 :modals/close
 (fn [{:keys [db]} _]
   {:fx            [[:dispatch
                     [:modals/delete
                      (-> (:modals db)
                          last
                          :id)]]
                    (when (<= (count (:modals db)) 1)
                      [:dispatch
                       [:layout/hide-bg-overlay]])]
    :body-overflow false}))

(rf/reg-event-fx
 :modals/open
 (fn [_ [_ child]]
   {:fx            [[:dispatch
                     [:modals/add {:show? true :child child :id (nano-id)}]]
                    [:dispatch
                     [:layout/show-bg-overlay
                      {:extra-classes ["z-30"]
                       :on-click      #(rf/dispatch [:modals/close])}
                      true]]]
    :body-overflow true}))
