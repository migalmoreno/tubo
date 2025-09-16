(ns tubo.stream.events
  (:require
   [re-frame.core :as rf]
   [tubo.layout.events :refer [show-loading-status]]
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
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]} [_ {:keys [body]}]]
   {:db (assoc db
               :stream
               (-> body
                   (utils/apply-thumbnails-quality
                    db
                    :related-streams)
                   (utils/apply-avatars-quality
                    db
                    :related-streams)
                   (utils/apply-image-quality
                    db
                    :uploader-avatar
                    :uploader-avatars)
                   (utils/apply-image-quality db
                                              :thumbnail
                                              :thumbnails)))
    :fx [(when (-> db
                   :settings
                   :show-comments)
           [:dispatch [:comments/fetch-page (:url body) [:stream]]])
         [:dispatch [:services/fetch body]]
         [:dispatch [:player/set-stream body main-player]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :stream/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:stream/load-page]
           [:bad-page-response [:stream/fetch-page url]]]]]
    :db (assoc db :stream nil)}))

(rf/reg-event-db
 :stream/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:stream layout]
    (not (-> db
             :stream
             layout)))))
