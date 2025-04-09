(ns tubo.comments.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :comments/fetch
 (fn [_ [_ url on-success on-error params]]
   {:fx [[:dispatch
          [:api/get (str "comments/" (js/encodeURIComponent url)) on-success
           on-error params]]]}))

(rf/reg-event-db
 :comments/load-page
 (fn [db [_ {:keys [body]}]]
   (-> db
       (assoc-in [:stream :comments-page] body)
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
 (fn [db [_ {:keys [body]}]]
   (-> db
       (update-in [:stream :comments-page :comments]
                  #(apply conj %1 %2)
                  (:comments body))
       (assoc-in [:stream :comments-page :next-page] (:next-page body))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :comments/bad-paginated-response
 (fn [{:keys [db]} [_ res]]
   {:fx [[:dispatch [:bad-response res]]]
    :db (assoc db :show-pagination-loading false)}))

(rf/reg-event-fx
 :comments/fetch-paginated
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch
            [:comments/fetch url
             [:comments/load-paginated] [:comments/bad-paginated-response]
             {:nextPage (js/encodeURIComponent next-page-url)}]]]})))
