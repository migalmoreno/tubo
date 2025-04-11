(ns tubo.search.events
  (:require
   [re-frame.core :as rf]
   [vimsical.re-frame.cofx.inject :as inject]))

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
                 :search/results    nil)
      :fx [[:dispatch [:search/show-form true]]
           [:dispatch [:search/hide-suggestions]]
           [:dispatch [:search/change-input query]]
           [:dispatch
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
         (assoc-in [:search/results :next-page] (:next-page body))))))

(rf/reg-event-fx
 :search/fetch-paginated
 (fn [{:keys [db]} [_ {:keys [query id next-page-url filter]}]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:fx [[:dispatch
            [:search/fetch id
             [:search/load-paginated] [:bad-pagination-response]
             (into
              {:q        query
               :nextPage (js/encodeURIComponent next-page-url)}
              (when filter
                {:filter filter}))]]]
      :db (assoc db :show-pagination-loading true)})))

(rf/reg-event-fx
 :search/leave-page
 (fn [{:keys [db]}]
   {:fx [(when-not (= (-> (:navigation/current-match db)
                          :data
                          :name)
                      :search-page)
           [:dispatch [:search/activate false]])
         [:dispatch [:search/change-filter nil]]]}))

(rf/reg-fx
 :focus-input!
 (fn [input]
   (.focus input)))

(rf/reg-event-fx
 :search/focus-input
 [(rf/inject-cofx ::inject/sub [:search-input])]
 (fn [{:keys [search-input]}]
   {:focus-input! @search-input}))

(rf/reg-fx
 :change-input!
 (fn [{:keys [input value]}]
   (set! (.-value input) value)))

(rf/reg-event-fx
 :search/change-input
 [(rf/inject-cofx ::inject/sub [:search-input])]
 (fn [{:keys [search-input]} [_ val]]
   {:change-input! {:input @search-input :value val}}))

(rf/reg-event-db
 :search/show-form
 (fn [db [_ show?]]
   (assoc db :search/show-form show?)))

(rf/reg-event-fx
 :search/activate
 (fn [_ [_ val]]
   {:fx [[:dispatch [:search/show-form val]]
         (if val
           [:dispatch-later [{:ms 100 :dispatch [:search/focus-input]}]]
           [:dispatch [:search/hide-suggestions]])]}))

(rf/reg-event-fx
 :search/fetch-suggestions
 (fn [{:keys [db]} [_ query on-success]]
   (when query
     {:fx [[:dispatch
            [:api/get (str "services/" (:service-id db) "/suggestions")
             on-success
             [:bad-response]
             {:q query}]]]})))

(rf/reg-event-db
 :search/change-suggestions
 (fn [db [_ {:keys [body]}]]
   (assoc db :search/suggestions {(:service-id db) body})))

(rf/reg-event-fx
 :search/load-suggestions
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db :search/suggestions {(:service-id db) body})
    :fx [[:dispatch [:search/show-suggestions true]]]}))

(rf/reg-event-fx
 :search/hide-suggestions
 (fn []
   {:fx [[:dispatch [:search/show-suggestions false]]
         [:dispatch [:layout/hide-bg-overlay]]]}))

(rf/reg-event-db
 :search/show-suggestions
 (fn [db [_ show?]]
   (assoc db :search/show-suggestions show?)))

(rf/reg-event-fx
 :search/focus-search
 (fn [{:keys [db]} [_ query]]
   {:fx (into [[:dispatch
                [:layout/show-bg-overlay
                 {:extra-classes ["z-10"]
                  :on-click      #(rf/dispatch [:search/hide-suggestions])}]]]
              (if (seq query)
                [[:dispatch [:search/show-suggestions true]]
                 (when-not (seq (get (:search/suggestions db) (:service-id db)))
                   [:dispatch
                    [:search/fetch-suggestions query
                     [:search/load-suggestions]]])]
                [[:dispatch [:search/show-suggestions false]]]))}))

(rf/reg-event-fx
 :search/set-debounced-query
 (fn [_ [_ query]]
   {:fx [[:dispatch [:search/set-query query]]
         (if (seq query)
           [:dispatch
            [:search/fetch-suggestions query [:search/load-suggestions]]]
           [:dispatch [:search/change-suggestions nil]])]}))

(rf/reg-event-db
 :search/set-query
 (fn [db [_ query]]
   (assoc db :search/query query)))

(rf/reg-event-fx
 :search/fill-query
 (fn [_ [_ query]]
   {:fx [[:dispatch [:search/change-query query]]
         [:dispatch [:search/change-input query]]
         [:dispatch [:search/focus-input]]]}))

(rf/reg-event-fx
 :search/change-query
 (fn [_ [_ query]]
   {:debounce {:id    :search/query
               :event [:search/set-debounced-query query]
               :time  200}}))

(rf/reg-event-fx
 :search/clear-query
 (fn []
   {:stop-debounce :search/query
    :fx            [[:dispatch [:search/fill-query nil]]
                    [:dispatch [:search/change-suggestions nil]]]}))

(rf/reg-event-fx
 :search/cancel
 (fn []
   {:stop-debounce :search/query
    :fx            [[:dispatch [:search/activate false]]]}))

(rf/reg-event-db
 :search/change-filter
 (fn [db [_ filter]]
   (assoc db :search/filter filter)))

(rf/reg-event-fx
 :search/submit
 (fn [{:keys [db]} [_ query]]
   (when (seq query)
     {:stop-debounce :search/query
      :fx            [[:dispatch
                       [:search/fetch-suggestions query
                        [:search/change-suggestions]]]
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
