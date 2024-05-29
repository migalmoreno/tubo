(ns tubo.channel.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-fx
 :channel/fetch
 (fn [{:keys [db]} [_ uri on-success on-error]]
   (api/get-request
    (str "/channels/" (js/encodeURIComponent uri))
    on-success on-error)))

(rf/reg-event-fx
 :channel/load-page
 (fn [{:keys [db]} [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :channel channel-res
                 :show-page-loading false)
      :fx [[:dispatch [:services/fetch channel-res]]
           [:document-title (:name channel-res)]]})))

(rf/reg-event-fx
 :channel/fetch-page
 (fn [{:keys [db]} [_ uri]]
   {:fx [[:dispatch [:channel/fetch uri [:channel/load-page] [:bad-response]]]]
    :db (assoc db :show-page-loading true)}))

(rf/reg-event-db
 :channel/load-paginated
 (fn [db [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     (if (empty? (:related-streams channel-res))
       (-> db
           (assoc-in [:channel :next-page] nil)
           (assoc :show-pagination-loading false))
       (-> db
           (update-in [:channel :related-streams] #(apply conj %1 %2)
                      (:related-streams channel-res))
           (assoc-in [:channel :next-page] (:next-page channel-res))
           (assoc :show-pagination-loading false))))))

(rf/reg-event-fx
 :channel/fetch-paginated
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/channels/" (js/encodeURIComponent uri) )
       [:channel/load-paginated] [:bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))
