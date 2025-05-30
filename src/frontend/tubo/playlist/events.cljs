(ns tubo.playlist.events
  (:require
   [re-frame.core :as rf]
   [tubo.utils :as utils]))

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
                  (-> body
                      (utils/apply-thumbnails-quality db :related-streams)
                      (utils/apply-avatars-quality db :related-streams)
                      :related-streams))
       (assoc-in [:playlist :next-page] (:next-page body))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :playlist/fetch-paginated
 (fn [{:keys [db]} [_ url next-page]]
   (if (seq next-page)
     {:fx [[:dispatch
            [:playlist/fetch url
             [:playlist/load-paginated] [:bad-pagination-response]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]
      :db (assoc db :show-pagination-loading true)}
     {:db (assoc db :show-pagination-loading false)})))

(rf/reg-event-fx
 :playlist/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :playlist          (-> body
                                      (utils/apply-image-quality
                                       db
                                       :thumbnail
                                       :thumbnails)
                                      (utils/apply-image-quality
                                       db
                                       :uploader-avatar
                                       :uploader-avatars)
                                      (utils/apply-thumbnails-quality
                                       db
                                       :related-streams)
                                      (utils/apply-avatars-quality
                                       db
                                       :related-streams))
               :show-page-loading false)
    :fx [[:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :playlist/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:playlist/fetch url
           [:playlist/load-page]
           [:bad-page-response [:playlist/fetch-page url]]]]]
    :db (assoc db
               :show-page-loading true
               :playlist          nil)}))
