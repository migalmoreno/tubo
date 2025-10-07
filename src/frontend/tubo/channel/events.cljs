(ns tubo.channel.events
  (:require
   [re-frame.core :as rf]
   [tubo.channel.views :as channel]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :channel/fetch
 [(show-loading-status :api/get)]
 (fn [_ [_ uri on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "channels/" (js/encodeURIComponent uri)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :channel/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :channel
               (-> body
                   (utils/apply-thumbnails-quality db :related-items)
                   (utils/apply-avatars-quality db :related-items)
                   (utils/apply-image-quality db :avatar :avatars)
                   (utils/apply-image-quality db :banner :banners)))
    :fx [[:dispatch [:services/fetch body]]
         [:dispatch
          [:channel/fetch-tab (:url body)
           (first (:content-filters (first (:tabs body))))]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :channel/fetch-page
 (fn [{:keys [db]} [_ uri]]
   {:fx [[:dispatch [:change-view channel/channel]]
         [:dispatch
          [:channel/fetch uri [:channel/load-page]
           [:bad-page-response [:channel/fetch-page uri]]]]]
    :db (assoc db :channel nil)}))

(rf/reg-event-fx
 :channel/load-tab
 (fn [{:keys [db]} [_ tab-id {:keys [body]}]]
   (let [selected-tab (first (filter #(= tab-id (first (:content-filters %)))
                                     (get-in db [:channel :tabs])))
         tab-idx      (.indexOf (get-in db [:channel :tabs]) selected-tab)]
     {:db (-> db
              (assoc-in [:channel :tabs tab-idx :related-items]
                        (-> body
                            (utils/apply-thumbnails-quality db :related-items)
                            (utils/apply-avatars-quality db :related-items)
                            :related-items))
              (assoc-in [:channel :tabs tab-idx :next-page]
                        (:next-page body)))})))

(rf/reg-event-fx
 :channel/fetch-tab
 (fn [_ [_ uri tab-id]]
   {:fx [(when tab-id
           [:dispatch
            [:api/get
             (str "channels/" (js/encodeURIComponent uri) "/tabs/" tab-id)
             [:channel/load-tab tab-id] [:bad-response]]])]}))

(rf/reg-event-db
 :channel/load-tab-paginated
 (fn [db [_ tab-id {:keys [body]}]]
   (let [selected-tab (first (filter #(= tab-id (first (:content-filters %)))
                                     (get-in db [:channel :tabs])))
         tab-idx      (.indexOf (get-in db [:channel :tabs]) selected-tab)]
     (if (seq (:related-items selected-tab))
       (-> db
           (update-in [:channel :tabs tab-idx :related-items]
                      #(apply conj %1 %2)
                      (-> body
                          (utils/apply-thumbnails-quality db :items)
                          (utils/apply-avatars-quality db :items)
                          :items))
           (assoc-in [:channel :tabs tab-idx :next-page] (:next-page body))
           (assoc :show-pagination-loading false))
       (-> db
           (assoc-in [:channel :tabs tab-idx :next-page] nil)
           (assoc :show-pagination-loading false))))))

(rf/reg-event-fx
 :channel/bad-tab-paginated-response
 (fn [{:keys [db]} [_ tab-id res]]
   (let [selected-tab (first (filter #(= tab-id (first (:content-filters %)))
                                     (get-in db [:channel :tabs])))
         tab-idx      (.indexOf (get-in db [:channel :tabs]) selected-tab)]
     {:fx [[:dispatch [:bad-pagination-response res]]]
      :db (assoc-in db
           (if tab-id
             [:channel :tabs tab-idx :next-page]
             [:channel :next-page])
           nil)})))

(rf/reg-event-fx
 :channel/fetch-tab-paginated
 (fn [{:keys [db]} [_ uri tab-id next-page]]
   (if (seq next-page)
     {:fx [[:dispatch
            [:api/get
             (str "channels/" (js/encodeURIComponent uri)
                  "/tabs/"    tab-id)
             [:channel/load-tab-paginated tab-id]
             [:channel/bad-tab-paginated-response tab-id]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]
      :db (assoc db :show-pagination-loading true)}
     {:db (assoc db :show-pagination-loading false)})))
