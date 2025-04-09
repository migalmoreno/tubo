(ns tubo.playlist.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :playlist/fetch
 (fn [_ [_ url on-success on-error params]]
   {:fx [[:dispatch
          [:api/get (str "playlists/" (js/encodeURIComponent url)) on-success
           on-error params]]]}))

(rf/reg-event-db
 :playlist/load-paginated
 (fn [db [_ {:keys [body]}]]
   (-> db
       (update-in [:playlist :related-streams]
                  #(apply conj %1 %2)
                  (:related-streams body))
       (assoc-in [:playlist :next-page] (:next-page body))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :playlist/fetch-paginated
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:fx [[:dispatch
            [:playlist/fetch url
             [:playlist/load-paginated] [:bad-response]
             {:nextPage (js/encodeURIComponent next-page-url)}]]]
      :db (assoc db :show-pagination-loading true)})))

(rf/reg-event-fx
 :playlist/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :playlist          body
               :show-page-loading false)
    :fx [[:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :playlist/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:playlist/fetch url
           [:playlist/load-page] [:bad-response]]]]
    :db (assoc db
               :show-page-loading true
               :playlist          nil)}))
