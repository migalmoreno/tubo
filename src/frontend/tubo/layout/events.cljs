(ns tubo.layout.events
  (:require
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn show-loading-status
  [key]
  (rf/->interceptor
   :id    :show-loading-status
   :after (fn [context]
            (update-in
             context
             [:effects :fx]
             (fn [fx]
               (conj
                (if fx
                  (map
                   #(map (fn [dispatched-fx]
                           (if (and (coll? dispatched-fx)
                                    (= (first dispatched-fx) key))
                             (update dispatched-fx
                                     2
                                     (fn [on-success] [:on-success
                                                       on-success]))
                             dispatched-fx))
                         %)
                   fx)
                  [])
                (when (some (fn [fx]
                              (some #(and (coll? %) (= (first %) key)) fx))
                            (get-in context [:effects :fx]))
                  [:dispatch [:start-loading]])))))))

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
         [:dispatch [:layout/show-bg-overlay {:extra-classes ["z-40"]}]]]}))

(rf/reg-event-fx
 :layout/show-mobile-panel
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :layout/mobile-panel (assoc data :show? true))
    :fx [[:dispatch [:layout/register-panel data]]]}))

(defn default-popover-data
  []
  {:id                    (nano-id)
   :destroy-on-click-out? true})

(rf/reg-event-db
 :layout/register-panel
 (fn [db [_ data]]
   (let [full-data (merge (default-popover-data) data)]
     (assoc-in db [:layout/panels (:id data)] full-data))))

(rf/reg-event-db
 :layout/destroy-panels-by-ids
 (fn [db [_ ids]]
   (update db :layout/panels #(apply dissoc % ids))))

(rf/reg-event-db
 :layout/register-tooltip
 (fn [db [_ data]]
   (let [full-data (merge (default-popover-data) data)]
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

(def tooltip-class-prefix "tooltip-controller-")
(def panel-class-prefix "panel-controller-")

(defn find-tooltip-controller-class-in-node
  [node class-prefix]
  (some->> (.-className node)
           (re-find (re-pattern (str class-prefix "([\\w\\-]+)")))
           (first)))

(defn find-tooltip-controller-class
  [node class-prefix]
  (or (find-tooltip-controller-class-in-node node class-prefix)
      (some-> (.-parentNode node)
              (find-tooltip-controller-class class-prefix))))

(defn find-clicked-controller-id
  [node class-prefix]
  (some->
    (find-tooltip-controller-class node class-prefix)
    (str/split class-prefix)
    (second)))

(rf/reg-event-fx
 :layout/destroy-tooltips-on-click-out
 (fn [{:keys [db]} [_ clicked-node]]
   (when (seq (:layout/tooltips db))
     (let [clicked-controller (find-clicked-controller-id clicked-node
                                                          tooltip-class-prefix)
           tooltips-ids       (->> (:layout/tooltips db)
                                   (vals)
                                   (filter :destroy-on-click-out?)
                                   (map :id)
                                   (set))]
       {:fx [[:dispatch
              [:layout/destroy-tooltips-by-ids
               (disj tooltips-ids clicked-controller)]]]}))))

(rf/reg-event-fx
 :layout/destroy-panels-on-click-out
 (fn [{:keys [db]} [_ clicked-node]]
   (when (and (seq (:layout/panels db)) (not (seq (:layout/tooltips db))))
     (let [clicked-controller (find-clicked-controller-id clicked-node
                                                          panel-class-prefix)
           panels-ids         (->> (:layout/panels db)
                                   (vals)
                                   (filter :destroy-on-click-out?)
                                   (map :id)
                                   (set))]
       {:fx [[:dispatch
              [:layout/destroy-panels-by-ids
               (disj panels-ids clicked-controller)]]]}))))

(rf/reg-fx
 :intersection-observer
 (fn [{:keys [observer elem cb opts]}]
   (when @observer
     (.disconnect @observer))
   (when elem
     (.observe
      (reset! observer (js/IntersectionObserver. cb (clj->js opts)))
      elem))))

(rf/reg-event-fx
 :layout/add-intersection-observer
 (fn [{:keys [db]} [_ observer elem cb opts]]
   (when-not (:show-pagination-loading db)
     {:intersection-observer
      {:observer observer :elem elem :cb cb :opts opts}})))
