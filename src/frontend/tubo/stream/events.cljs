(ns tubo.stream.events
  (:require
   [tubo.layout.views :as layout]))

(rf/reg-event-fx
 :stream/fetch
 (fn [_ [_ url on-success on-error]]
   {:fx [[:dispatch
          [:api/get (str "streams/" (js/encodeURIComponent url)) on-success
           on-error]]]}))

(rf/reg-event-fx
 :stream/load-page
(rf/reg-event-fx
 :stream/bad-page-response
 (fn [{:keys [db]} [_ url res]]
   {:fx [[:dispatch
          [:change-view #(layout/error res [:stream/fetch-page url])]]]
    :db (assoc db :show-page-loading false)}))
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :stream            body
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
           [:stream/load-page] [:stream/bad-page-response url]]]]
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
