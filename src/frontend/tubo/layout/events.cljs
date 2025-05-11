(ns tubo.layout.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(rf/reg-event-db
 :layout/show-bg-overlay
 (fn [db [_ {:keys [on-click] :as data} remain-open?]]
   (assoc db
          :layout/bg-overlay
          (assoc data
                 :show?    true
                 :on-click #(do (when on-click (on-click))
                                (when-not remain-open?
                                  (rf/dispatch [:layout/hide-bg-overlay])))))))

(rf/reg-event-db
 :layout/hide-bg-overlay
 (fn [db _]
   (assoc-in db [:layout/bg-overlay :show?] false)))

(rf/reg-event-fx
 :layout/show-mobile-tooltip
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :layout/mobile-tooltip (assoc data :show? true))
    :fx [[:dispatch
          [:layout/register-tooltip data]]
         [:dispatch [:layout/show-bg-overlay {:extra-classes ["z-30"]}]]]}))

(defn default-tooltip-data
  []
  {:id                    (nano-id)
   :destroy-on-click-out? true})

(rf/reg-event-db
 :layout/register-tooltip
 (fn [db [_ data]]
   (let [full-data (merge (default-tooltip-data) data)]
     (assoc-in db [:layout/tooltips (:id data)] full-data))))

(rf/reg-event-fx
 :layout/destroy-tooltip-by-id
 (fn [{:keys [db]} [_ id]]
   {:db (update db :layout/tooltips dissoc id)}))

(rf/reg-event-fx
 :layout/change-tooltip-items
 (fn [{:keys [db]} [_ id items]]
   {:db (update-in db [:layout/tooltips id] #(assoc %1 :items %2) items)}))

(rf/reg-event-db
 :layout/destroy-tooltips-by-ids
 (fn [db [_ ids]]
   (update db :layout/tooltips #(apply dissoc % ids))))

(defonce tooltip-controller-class-prefix "tooltip-controller-")

(defn find-tooltip-controller-class-in-node
  [node]
  (some->> (.-className node)
           (re-find (re-pattern (str tooltip-controller-class-prefix
                                     "([\\w\\-]+)")))
           (first)))

(defn find-tooltip-controller-class
  [node]
  (or (find-tooltip-controller-class-in-node node)
      (some-> (.-parentNode node)
              (find-tooltip-controller-class))))

(defn find-clicked-controller-id
  [node]
  (some->
    (find-tooltip-controller-class node)
    (str/split tooltip-controller-class-prefix)
    (second)))

(rf/reg-event-fx
 :layout/destroy-tooltips-on-click-out
 (fn [{:keys [db]} [_ clicked-node]]
   (when (seq (:layout/tooltips db))
     (let [clicked-controller (find-clicked-controller-id clicked-node)
           tooltip-ids        (->> (:layout/tooltips db)
                                   (vals)
                                   (filter :destroy-on-click-out?)
                                   (map :id)
                                   (set))]
       {:fx [[:dispatch
              [:layout/destroy-tooltips-by-ids
               (disj tooltip-ids clicked-controller)]]]}))))
