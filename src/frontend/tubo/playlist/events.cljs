(ns tubo.playlist.events
  (:require
   [re-frame.core :as rf]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :playlist/fetch
 [(show-loading-status :api/get)]
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
            [:api/get (str "playlists/" (js/encodeURIComponent url))
             [:playlist/load-paginated] [:bad-pagination-response]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]
      :db (assoc db :show-pagination-loading true)}
     {:db (assoc db :show-pagination-loading false)})))

(rf/reg-event-fx
 :playlist/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :playlist
               (-> body
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
                    :related-streams)))
    :fx [[:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :playlist/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:playlist/fetch url
           [:playlist/load-page]
           [:bad-page-response [:playlist/fetch-page url]]]]]
    :db (assoc db :playlist nil)}))

(rf/reg-event-fx
 :playlist/fetch-related-streams
 (fn [_ [_ url]]
   {:fx [[:dispatch
          [:api/get (str "playlists/" (js/encodeURIComponent url))
           [:bg-player/load-related-streams true] [:bad-response]]]]}))

(rf/reg-event-fx
 :playlist/play-all
 (fn [{:keys [db]} [_ streams]]
   {:fx [[:dispatch [:queue/add-n streams false (count (:queue db))]]
         [:dispatch
          [:bg-player/fetch-stream (:url (first streams))
           (count (:queue db))
           true]]]}))

(rf/reg-event-fx
 :playlist/shuffle-all
 (fn [{:keys [db]} [_ streams]]
   (let [shuffled-streams (shuffle streams)]
     {:fx [[:dispatch
            [:queue/add-n (shuffle streams) false (count (:queue db))]]
           [:dispatch
            [:bg-player/fetch-stream (:url (first shuffled-streams))
             (count (:queue db))
             true]]]})))
