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
 (fn [db [_ keys {:keys [body]}]]
   (-> db
       (assoc-in
        (into keys [:comments-page])
        (-> (utils/apply-avatars-quality body db :related-items)
            (update :related-items
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
       (assoc-in (into keys [:show-comments-loading]) false))))

(rf/reg-event-fx
 :bad-comments-response
 (fn [{:keys [db]} [_ keys res]]
   {:fx [[:dispatch [:bad-response res]]]
    :db (assoc-in db (into keys [:show-comments-loading]) false)}))

(rf/reg-event-fx
 :comments/fetch-page
 (fn [{:keys [db]} [_ url keys]]
   {:fx [[:dispatch
          [:comments/fetch url
           [:comments/load-page keys] [:bad-comments-response keys]]]]
    :db (-> db
            (assoc-in (into keys [:show-comments-loading]) true)
            (assoc-in (into keys [:show-comments]) true))}))

(rf/reg-event-fx
 :comments/load-replies
 (fn [{:keys [db]} [_ comment-id {:keys [body]}]]
   {:db (update-in db
                   (into (if (:main-player/show db)
                           [:queue (:queue/position db)]
                           [:stream])
                         [:comments-page :related-items])
                   (fn [comments]
                     (map (fn [comment]
                            (if (= (:comment-id comment) comment-id)
                              (assoc comment
                                     :replies (utils/apply-avatars-quality
                                               body
                                               db
                                               :items)
                                     :replies-loading false)
                              comment))
                          comments)))}))

(rf/reg-event-fx
 :comments/fetch-replies
 (fn [{:keys [db]} [_ comment-id url replies-page]]
   {:db (update-in
         db
         (into
          (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
          [:comments-page :related-items])
         (fn [comments]
           (map #(if (= (:comment-id %) comment-id)
                   (assoc % :replies-loading true)
                   %)
                comments)))
    :fx [[:dispatch
          [:comments/fetch url
           [:comments/load-replies comment-id] [:bad-response]
           {:nextPage (.stringify js/JSON (clj->js replies-page))}]]
         [:dispatch [:comments/show-replies comment-id true]]]}))

(rf/reg-event-fx
 :comments/load-more-replies
 (fn [{:keys [db]} [_ comment-id {:keys [body]}]]
   {:db (update-in db
                   (into (if (:main-player/show db)
                           [:queue (:queue/position db)]
                           [:stream])
                         [:comments-page :related-items])
                   (fn [comments]
                     (map (fn [comment]
                            (if (= (:comment-id comment) comment-id)
                              (-> comment
                                  (assoc-in [:replies :next-page]
                                            (:next-page body))
                                  (update-in [:replies :items]
                                             #(into (into [] %1) %2)
                                             (:items
                                              (utils/apply-avatars-quality
                                               body
                                               db
                                               :items)))
                                  (assoc :replies-loading false))
                              comment))
                          comments)))}))

(rf/reg-event-fx
 :comments/fetch-more-replies
 (fn [{:keys [db]} [_ comment-id url replies-page]]
   {:db (update-in
         db
         (into
          (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
          [:comments-page :related-items])
         (fn [comments]
           (map #(if (= (:comment-id %) comment-id)
                   (assoc % :replies-loading true)
                   %)
                comments)))
    :fx [[:dispatch
          [:comments/fetch url
           [:comments/load-more-replies comment-id] [:bad-response]
           {:nextPage (.stringify js/JSON (clj->js replies-page))}]]]}))

(rf/reg-event-fx
 :comments/show-replies
 (fn [{:keys [db]} [_ comment-id val]]
   {:db
    (update-in
     db
     (into (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
           [:comments-page :related-items])
     (fn [comments]
       (map #(if (= (:comment-id %) comment-id)
               (assoc % :show-replies val)
               %)
            comments)))}))

(rf/reg-event-db
 :comments/load-paginated
 (fn [db [_ {:keys [body]}]]
   (->
     db
     (update-in
      (into (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
            [:comments-page :related-items])
      #(into (into [] %1) (into [] %2))
      (-> body
          (utils/apply-avatars-quality db :items)
          :items))
     (assoc-in
      (into (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
            [:comments-page :next-page])
      (:next-page body))
     (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 :comments/fetch-paginated
 (fn [{:keys [db]} [_ url next-page]]
   (if (seq next-page)
     {:db (assoc db :show-pagination-loading true)
      :fx [[:dispatch
            [:comments/fetch url
             [:comments/load-paginated] [:bad-pagination-response]
             {:nextPage (.stringify js/JSON (clj->js next-page))}]]]}
     {:db (assoc db :show-pagination-loading false)})))
