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

(rf/reg-event-fx
 :channel/load-tab
 (fn [{:keys [db]} [_ tab-id res]]
   (let [tab-res      (js->clj res :keywordize-keys true)
         selected-tab (first (filter #(= tab-id
                                         (-> %
                                             :contentFilters
                                             first))
                                     (-> db
                                         :channel
                                         :tabs)))
         tab-idx      (.indexOf (-> db
                                    :channel
                                    :tabs)
                                selected-tab)]
     {:db (-> db
              (assoc-in [:channel :tabs tab-idx :related-streams]
                        (:related-streams tab-res))
              (assoc-in [:channel :tabs tab-idx :next-page]
                        (:next-page tab-res)))})))

(rf/reg-event-fx
 :channel/fetch-tab
 (fn [_ [_ uri tab-id]]
   (api/get-request
    (str "/channels/" (js/encodeURIComponent uri) "/tabs/" tab-id)
    [:channel/load-tab tab-id]
    [:channel/bad-page-response])))

(rf/reg-event-db
 :channel/load-paginated
 (fn [db [_ tab-id res]]
   (let [channel-res  (js->clj res :keywordize-keys true)
         selected-tab (first (filter #(= tab-id
                                         (-> %
                                             :contentFilters
                                             first))
                                     (-> db
                                         :channel
                                         :tabs)))
         tab-idx      (.indexOf (-> db
                                    :channel
                                    :tabs)
                                selected-tab)]
     (if (empty? (:related-streams (if tab-id selected-tab channel-res)))
       (-> db
           (assoc-in (if tab-id
                       [:channel :tabs tab-idx :next-page]
                       [:channel :next-page])
                     nil)
           (assoc :show-pagination-loading false))
       (-> db
           (update-in (if tab-id
                        [:channel :tabs tab-idx :related-streams]
                        [:channel :related-streams])
                      #(apply conj %1 %2)
                      (:related-streams channel-res))
           (assoc-in (if tab-id
                       [:channel :tabs tab-idx :next-page]
                       [:channel :next-page])
                     (:next-page channel-res))
           (assoc :show-pagination-loading false))))))

(rf/reg-event-fx
 :channel/bad-paginated-response
 (fn [{:keys [db]} [_ tab-id res]]
   (let [selected-tab (first (filter #(= tab-id
                                         (-> %
                                             :contentFilters
                                             first))
                                     (-> db
                                         :channel
                                         :tabs)))
         tab-idx      (.indexOf (-> db
                                    :channel
                                    :tabs)
                                selected-tab)]
     {:fx [[:dispatch [:bad-response res]]]
      :db (-> db
              (assoc-in (if tab-id
                          [:channel :tabs tab-idx :next-page]
                          [:channel :next-page])
                        nil)
              (assoc :show-pagination-loading false))})))

(rf/reg-event-fx
 :channel/fetch-paginated
 (fn [{:keys [db]} [_ uri tab-id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/channels/" (js/encodeURIComponent uri)
            "/tabs/"     (or tab-id "default"))
       [:channel/load-paginated tab-id]
       [:channel/bad-paginated-response tab-id]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db
      (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 :channel/reset
 (fn [db _]
   (assoc db :channel nil)))
