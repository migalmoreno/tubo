(ns tubo.comments.events
  (:require
   [re-frame.core :as rf]
   [tubo.utils :as utils]))

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
       (assoc-in
        [:stream :comments-page]
        (-> (utils/apply-avatars-quality body db :comments)
            (update :comments
                    #(map
                      (fn [c]
                        (if (seq (:replies c))
                          (update-in c
                                     [:replies :items]
                                     (fn [items]
                                       (map (fn [i]
                                              (utils/apply-image-quality
                                               i
                                               db
                                               :uploader-avatar
                                               :uploader-avatars))
                                            items)))
                          c))
                      %))))
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
                  #(into (into [] %1) (into [] %2))
                  (-> body
                      (utils/apply-avatars-quality db :comments)
                      :comments))
       (assoc-in [:stream :comments-page :next-page] (:next-page body))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :comments/fetch-paginated
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch
            [:comments/fetch url
             [:comments/load-paginated] [:bad-pagination-response]
             {:nextPage (js/encodeURIComponent next-page-url)}]]]})))
