(ns tubo.channel.events
  (:require
   [re-frame.core :as rf]
   [tubo.channel.views :as channel]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :channel/fetch
 (fn [_ [_ uri on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "channels/" (js/encodeURIComponent uri)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :channel/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :channel           (-> body
                                      (utils/apply-thumbnails-quality
                                       db
                                       :related-streams)
                                      (utils/apply-avatars-quality
                                       db
                                       :related-streams)
                                      (utils/apply-image-quality db
                                                                 :avatar
                                                                 :avatars)
                                      (utils/apply-image-quality db
                                                                 :banner
                                                                 :banners))
               :show-page-loading false)
    :fx [[:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :channel/fetch-page
 (fn [{:keys [db]} [_ uri]]
   {:fx [[:dispatch [:change-view channel/channel]]
         [:dispatch
          [:channel/fetch uri [:channel/load-page]
           [:bad-page-response [:channel/fetch-page uri]]]]]
    :db (assoc db :show-page-loading true)}))

(rf/reg-event-fx
 :channel/load-tab
 (fn [{:keys [db]} [_ tab-id {:keys [body]}]]
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
     {:db (-> db
              (assoc-in [:channel :tabs tab-idx :related-streams]
                        (-> body
                            (utils/apply-thumbnails-quality db :related-streams)
                            (utils/apply-avatars-quality db :related-streams)
                            :related-streams))
              (assoc-in [:channel :tabs tab-idx :next-page]
                        (:next-page body)))})))

(rf/reg-event-fx
 :channel/fetch-tab
 (fn [_ [_ uri tab-id]]
   {:fx [[:dispatch
          [:api/get
           (str "channels/" (js/encodeURIComponent uri) "/tabs/" tab-id)
           [:channel/load-tab tab-id]
           [:bad-page-response [:channel/fetch-page uri]]]]]}))

(rf/reg-event-db
 :channel/load-paginated
 (fn [db [_ tab-id {:keys [body]}]]
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
     (if (empty? (:related-streams (if tab-id selected-tab body)))
       (-> db
           (assoc-in (if tab-id
                       [:channel :tabs tab-idx :next-page]
                       [:channel :next-page])
                     nil)
           (assoc :show-pagination-loading false))
       (-> db
           (update-in
            (if tab-id
              [:channel :tabs tab-idx :related-streams]
              [:channel :related-streams])
            #(apply conj %1 %2)
            (-> body
                (utils/apply-thumbnails-quality db :related-streams)
                (utils/apply-avatars-quality db :related-streams)
                :related-streams))
           (assoc-in (if tab-id
                       [:channel :tabs tab-idx :next-page]
                       [:channel :next-page])
                     (:next-page body))
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
     {:fx [[:dispatch [:bad-pagination-response res]]]
      :db (assoc-in db
           (if tab-id
             [:channel :tabs tab-idx :next-page]
             [:channel :next-page])
           nil)})))

(rf/reg-event-fx
 :channel/fetch-paginated
 (fn [{:keys [db]} [_ uri tab-id next-page]]
   (if (seq next-page)
     {:fx [[:dispatch
            [:api/get
             (str "channels/" (js/encodeURIComponent uri)
                  "/tabs/"    (or tab-id "default"))
             [:channel/load-paginated tab-id]
             [:channel/bad-paginated-response tab-id]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]
      :db (assoc db :show-pagination-loading true)}
     {:db (assoc db :show-pagination-loading false)})))
