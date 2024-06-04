(ns tubo.playlist.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-fx
 :playlist/fetch
 (fn [{:keys [db]} [_ url on-success on-error params]]
   (api/get-request (str "/playlists/" (js/encodeURIComponent url))
                    on-success on-error params)))

(rf/reg-event-db
 :playlist/load-paginated
 (fn [db [_ res]]
   (-> db
       (update-in [:playlist :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:playlist :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :playlist/fetch-paginated
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:fx [[:dispatch [:playlist/fetch url
                       [:playlist/load-paginated] [:bad-response]
                       {:nextPage (js/encodeURIComponent next-page-url)}]]]
      :db (assoc db :show-pagination-loading true)})))

(rf/reg-event-fx
 :playlist/load-page
 (fn [{:keys [db]} [_ res]]
   (let [playlist-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :playlist playlist-res
                 :show-page-loading false)
      :fx [[:dispatch [:services/fetch playlist-res]]
           [:document-title (:name playlist-res)]]})))

(rf/reg-event-fx
 :playlist/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch [:playlist/fetch url
                     [:playlist/load-page] [:bad-response]]]]
    :db (assoc db
               :show-page-loading true
               :playlist nil)}))
