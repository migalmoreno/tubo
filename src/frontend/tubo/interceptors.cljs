(ns tubo.interceptors
  (:require
   [re-frame.core :as rf]))

(def persist
  (rf/->interceptor
   :id    :persist
   :after (fn [context]
            (update-in context
                       [:effects :fx]
                       #(conj (or % [])
                              [:debounce
                               {:id    :storage
                                :event [:persist]
                                :time  500}])))))

(def schema-validator
  (rf/->interceptor
   :id    :schema-validator
   :after (fn [context]
            (update-in context
                       [:effects :fx]
                       #(conj (or % []) [:validate (:coeffects context)])))))

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

(comment
  (rf/reg-global-interceptor schema-validator)
  (rf/clear-global-interceptor :schema-validator))
