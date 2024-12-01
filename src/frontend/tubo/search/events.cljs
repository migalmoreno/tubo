(ns tubo.search.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]
   [tubo.layout.views :as layout]))

(rf/reg-event-fx
 :search/fetch
 (fn [_ [_ service-id on-success on-error params]]
   (api/get-request (str "/services/" service-id "/search")
                    on-success
                    on-error
                    params)))

(rf/reg-event-fx
 :search/load-page
 (fn [{:keys [db]} [_ res]]
   (let [search-res (js->clj res :keywordize-keys true)]
     {:db (assoc db
                 :search/results    search-res
                 :show-page-loading false)
      :fx [[:dispatch [:services/fetch search-res]]]})))

(rf/reg-event-fx
 :search/bad-page-response
 (fn [{:keys [db]} [_ service-id query res]]
   {:fx [[:dispatch
          [:change-view
           #(layout/error res [:search/fetch-page service-id query])]]]
    :db (assoc db :show-page-loading false)}))

(rf/reg-event-fx
 :search/fetch-page
 (fn [{:keys [db]} [_ service-id query]]
   {:db (assoc db
               :show-page-loading true
               :search/show-form  true
               :search/results    nil)
    :fx [[:dispatch
          [:search/fetch service-id
           [:search/load-page] [:search/bad-page-response service-id query]
           {:q query}]]
         [:document-title (str "Search for \"" query "\"")]]}))

(rf/reg-event-db
 :search/load-paginated
 (fn [db [_ res]]
   (let [search-res (js->clj res :keywordize-keys true)]
     (if (empty? (:items search-res))
       (-> db
           (assoc-in [:search/results :next-page] nil)
           (assoc :show-pagination-loading false))
       (-> db
           (update-in [:search/results :items]
                      #(apply conj %1 %2)
                      (:items search-res))
           (assoc-in [:search/results :next-page] (:next-page search-res))
           (assoc :show-pagination-loading false))))))

(rf/reg-event-fx
 :search/fetch-paginated
 (fn [{:keys [db]} [_ query id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:fx [[:dispatch
            [:search/fetch id
             [:search/load-paginated] [:bad-response]
             {:q        query
              :nextPage (js/encodeURIComponent next-page-url)}]]]
      :db (assoc db :show-pagination-loading true)})))

(rf/reg-event-db
 :search/show-form
 (fn [db [_ show?]]
   (when-not (= (-> db
                    :navigation/current-match
                    :path)
                "search")
     (assoc db :search/show-form show?))))

(rf/reg-event-db
 :search/change-query
 (fn [db [_ res]]
   (assoc db :search/query res)))
