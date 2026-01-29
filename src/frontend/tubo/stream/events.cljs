(ns tubo.stream.events
  (:require
   [re-frame.core :as rf]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.stream.views :as views]
   [tubo.utils :as utils]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :stream/fetch
 [(show-loading-status :api/get)]
 (fn [_ [_ url on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "streams/" (js/encodeURIComponent url)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :stream/load-page
 [(rf/inject-cofx ::inject/sub [:stream-player])]
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc
         db
         :stream
         (-> body
             (utils/apply-thumbnails-quality db :related-items)
             (utils/apply-avatars-quality db :related-items)
             (utils/apply-image-quality db :uploader-avatar :uploader-avatars)
             (utils/apply-image-quality db :thumbnail :thumbnails)))
    :fx [(when (get-in db [:settings :show-comments])
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
 (fn [{:keys [db]} [_ url on-success]]
   {:fx [[:dispatch
          [:stream/fetch url
           (or on-success [:stream/load-page])
           [:bad-page-response [:stream/on-reload url]]]]]
    :db (assoc db :stream nil)}))

(rf/reg-event-db
 :stream/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:stream layout]
    (not (-> db
             :stream
             layout)))))
