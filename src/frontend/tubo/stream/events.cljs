(ns tubo.stream.events
  (:require
   [re-frame.core :as rf]
   [tubo.interceptors :refer [show-loading-status]]
   [tubo.stream.views :as views]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :stream/fetch
 [(show-loading-status :api/get)]
 (fn [_ [_ url on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "streams/" (js/encodeURIComponent url)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :stream/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc
         db
         :stream
         (-> body
             (utils/apply-thumbnails-quality db :related-items)
             (utils/apply-avatars-quality db :related-items)
             (utils/apply-image-quality db :uploader-avatar :uploader-avatars)
             (utils/apply-image-quality db :thumbnail :thumbnails)))
    :fx [[:dispatch [:stream-player/load body]]
         (when (get-in db [:settings :show-comments])
           [:dispatch [:comments/fetch-page (:url body) [:stream]]])
         [:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :stream/reload-page
 (fn [_ [_ res]]
   {:fx [[:dispatch [:change-view views/stream-page]]
         [:dispatch [:stream/load-page res]]]}))

(rf/reg-event-fx
 :stream/on-reload
 (fn [_ [_ url]]
   {:fx [[:dispatch [:stream/fetch-page url [:stream/reload-page]]]]}))

(rf/reg-event-fx
 :stream/fetch-page
 (fn [_ [_ url on-success]]
   {:fx [[:dispatch
          [:stream/fetch url
           (or on-success [:stream/load-page])
           [:bad-page-response [:stream/on-reload url]]]]]}))

(rf/reg-event-fx
 :stream/leave-page
 (fn [{:keys [db]}]
   (when-not (= (get-in (:navigation/current-match db) [:data :name])
                :stream-page)
     {:db (assoc db :stream nil)})))

(rf/reg-event-db
 :stream/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db [:stream layout] (not (get-in db [:stream layout])))))
