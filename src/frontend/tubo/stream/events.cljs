(ns tubo.stream.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]
   [tubo.components.layout :as layout]))

(rf/reg-event-fx
 :stream/fetch
 (fn [_ [_ url on-success on-error]]
   (api/get-request (str "/streams/" (js/encodeURIComponent url))
                    on-success
                    on-error)))

(rf/reg-event-fx
 :stream/load-page
 (fn [{:keys [db]} [_ res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db
                 :stream            stream-res
                 :show-page-loading false)
      :fx [(when (-> db
                     :settings
                     :show-comments)
             [:dispatch [:comments/fetch-page (:url stream-res)]])
           [:dispatch [:services/fetch stream-res]]
           [:document-title (:name stream-res)]]})))

(rf/reg-event-fx
 :stream/bad-page-response
 (fn [{:keys [db]} [_ url res]]
   {:fx [[:dispatch
          [:change-view #(layout/error res [:stream/fetch-page url])]]]
    :db (assoc db :show-page-loading false)}))

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
