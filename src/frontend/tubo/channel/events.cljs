(ns tubo.channel.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]
   [tubo.channel.views :as channel]
   [tubo.layout.views :as layout]))

(rf/reg-event-fx
 :channel/fetch
 (fn [_ [_ uri on-success on-error]]
   (api/get-request
    (str "/channels/" (js/encodeURIComponent uri))
    on-success
    on-error)))

(rf/reg-event-fx
 :channel/load-page
 (fn [{:keys [db]} [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     {:db (assoc db
                 :channel           channel-res
                 :show-page-loading false)
      :fx [[:dispatch [:services/fetch channel-res]]
           [:document-title (:name channel-res)]]})))

(rf/reg-event-fx
 :channel/bad-page-response
 (fn [{:keys [db]} [_ uri res]]
   {:fx [[:dispatch
          [:change-view #(layout/error res [:channel/fetch-page uri])]]]
    :db (assoc db :show-page-loading false)}))

(rf/reg-event-fx
 :channel/fetch-page
 (fn [{:keys [db]} [_ uri]]
   {:fx [[:dispatch [:change-view channel/channel]]
         [:dispatch
          [:channel/fetch uri [:channel/load-page]
           [:channel/bad-page-response uri]]]]
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
           (update-in [:channel :related-streams]
                      #(apply conj %1 %2)
                      (:related-streams channel-res))
           (assoc-in [:channel :next-page] (:next-page channel-res))
           (assoc :show-pagination-loading false))))))

(rf/reg-event-fx
 :channel/bad-paginated-response
 (fn [{:keys [db]} [_ res]]
   {:fx [[:dispatch [:bad-response res]]]
    :db (assoc db :show-pagination-loading false)}))

(rf/reg-event-fx
 :channel/fetch-paginated
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/channels/" (js/encodeURIComponent uri))
       [:channel/load-paginated]
       [:channel/bad-paginated-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db
      (assoc db :show-pagination-loading true)))))
