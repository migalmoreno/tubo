(ns tubo.stream.events
  (:require
   [re-frame.core :as rf]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :stream/fetch
 (fn [_ [_ url on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "streams/" (js/encodeURIComponent url)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :stream/load-page
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :stream            (-> body
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
                                                                 :thumbnails))
               :show-page-loading false)
    :fx [(when (-> db
                   :settings
                   :show-comments)
           [:dispatch [:comments/fetch-page (:url body)]])
         [:dispatch [:services/fetch body]]
         [:document-title (:name body)]]}))

(rf/reg-event-fx
 :stream/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:stream/load-page] [:bad-page-response [:stream/fetch-page url]]]]]
    :db (assoc db
               :show-page-loading true
               :stream            nil)}))

(rf/reg-event-db
 :stream/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:stream layout]
    (not (-> db
             :stream
             layout)))))
