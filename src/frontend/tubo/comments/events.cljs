(ns tubo.comments.events
  (:require
   [re-frame.core :as rf]
   [tubo.api :as api]))

(rf/reg-event-fx
 :comments/fetch
 (fn [_ [_ url on-success on-error params]]
   (api/get-request (str "/comments/"
                         (js/encodeURIComponent url))
                    on-success
                    on-error
                    params)))

(rf/reg-event-db
 :comments/load-page
 (fn [db [_ res]]
   (-> db
       (assoc-in [:stream :comments-page] (js->clj res :keywordize-keys true))
       (assoc-in [:stream :show-comments-loading] false))))

(rf/reg-event-fx
 :comments/fetch-page
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:comments/fetch url
           [:comments/load-page] [:bad-response]]]]
    :db (-> db
            (assoc-in [:stream :show-comments-loading] true)
            (assoc-in [:stream :show-comments] true))}))

(rf/reg-event-db
 :comments/toggle-replies
 (fn [db [_ comment-id]]
   (update-in db
              [:stream :comments-page :comments]
              (fn [comments]
                (map #(if (= (:id %) comment-id)
                        (assoc % :show-replies (not (:show-replies %)))
                        %)
                     comments)))))

(rf/reg-event-db
 :comments/load-paginated
 (fn [db [_ res]]
   (-> db
       (update-in [:stream :comments-page :comments]
                  #(apply conj %1 %2)
                  (:comments (js->clj res :keywordize-keys true)))
       (assoc-in [:stream :comments-page :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :comments/fetch-paginated
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch
            [:comments/fetch url
             [:comments/load-paginated] [:bad-response]
             {:nextPage (js/encodeURIComponent next-page-url)}]]]})))
