(ns tubo.search.events
  (:require
   [re-frame.core :as rf]

(defonce !timeouts (atom {}))

(rf/reg-fx
 :debounce
 (fn [{:keys [id event time]}]
   (js/clearTimeout (get @!timeouts id))
   (swap! !timeouts assoc
     id
     (js/setTimeout (fn []
                      (rf/dispatch event)
                      (swap! !timeouts dissoc id))
                    time))))

(rf/reg-fx
 :stop-debounce
 (fn [id]
   (js/clearTimeout (get @!timeouts id))
   (swap! !timeouts dissoc id)))

(rf/reg-event-fx
 :search/fetch
 (fn [_ [_ service-id on-success on-error params]]
   {:fx [[:dispatch
          [:api/get (str "services/" service-id "/search") on-success on-error
           params]]]}))

(rf/reg-event-fx
 :search/load-page
 (fn [{:keys [db]} [_ {:keys [query filter]} {:keys [body]}]]
   {:db (assoc db
               :search/results    body
               :search/query      query
               :search/filter     filter
               :show-page-loading false)
    :fx [[:dispatch [:services/fetch body]]]}))

(rf/reg-event-fx
 :search/fetch-page
 (fn [{:keys [db]} [_ service-id query filter]]
   (let [default-filter (-> db
                            :settings
                            :default-filter
                            (get (js/parseInt service-id)))]
     {:db (assoc db
                 :show-page-loading true
                 :search/show-form  true
                 :search/results    nil)
      :fx [[:dispatch
            [:search/fetch service-id
             [:search/load-page {:query query :filter filter}]
             [:bad-page-response [:search/fetch-page service-id query]]
             (into {:q query}
                   (when (or (seq filter) default-filter)
                     {:filter (if (seq filter) filter default-filter)}))]]
           [:document-title (str "Search for \"" query "\"")]]})))

(rf/reg-event-db
 :search/load-paginated
 (fn [db [_ {:keys [body]}]]
   (if (empty? (:items body))
     (-> db
         (assoc-in [:search/results :next-page] nil)
         (assoc :show-pagination-loading false))
     (-> db
         (update-in [:search/results :items]
                    #(apply conj %1 %2)
                    (:items body))
         (assoc-in [:search/results :next-page] (:next-page body))
         (assoc :show-pagination-loading false)))))

(rf/reg-event-fx
 :search/fetch-paginated
 (fn [{:keys [db]} [_ {:keys [query id next-page-url filter]}]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:fx [[:dispatch
            [:search/fetch id
             [:search/load-paginated] [:bad-response]
             (into
              {:q        query
               :nextPage (js/encodeURIComponent next-page-url)}
              (when filter
                {:filter filter}))]]]
      :db (assoc db :show-pagination-loading true)})))

(rf/reg-event-fx
 :search/leave-page
 (fn []
   {:fx [[:dispatch [:search/show-form false]]
         [:dispatch [:search/change-filter nil]]]}))

(rf/reg-event-db
 :search/show-form
 (fn [db [_ show?]]
   (assoc db :search/show-form show?)))

(rf/reg-event-db
 :search/set-query
 (fn [db [_ query]]
   (assoc db :search/query query)))

(rf/reg-event-fx
 :search/change-query
 (fn [_ [_ query]]
   {:debounce {:id    :search/query
               :event [:search/set-query query]
               :time  250}}))

(rf/reg-event-fx
 :search/clear-query
 (fn []
   {:stop-debounce :search/query
    :fx            [[:dispatch [:search/change-query nil]]]}))

(rf/reg-event-fx
 :search/cancel
 (fn []
   {:stop-debounce :search/query
    :fx            [[:dispatch [:search/show-form false]]]}))

(rf/reg-event-db
 :search/change-filter
 (fn [db [_ filter]]
   (assoc db :search/filter filter)))

(rf/reg-event-fx
 :search/submit
 (fn [{:keys [db]} [_ query]]
   (when (seq query)
     {:stop-debounce :search/query
      :fx            [[:dispatch [:search/set-query query]]
                      [:dispatch
                       [:navigation/navigate
                        {:name   :search-page
                         :params {}
                         :query  (into {:q         query
                                        :serviceId (:service-id db)}
                                       (when (seq (:search/filter db))
                                         {:filter (:search/filter db)}))}]]]})))

(rf/reg-event-fx
 :search/set-filter
 (fn [{:keys [db]} [_ filter]]
   {:fx [[:dispatch
          [:navigation/navigate
           {:name   :search-page
            :params {}
            :query  {:serviceId (:service-id db)
                     :q         (:search/query db)
                     :filter    filter}}]]]}))
