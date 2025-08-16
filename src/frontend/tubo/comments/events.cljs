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
       (assoc-in (into keys [:show-comments-loading]) false))))

(rf/reg-event-fx
 :comments/fetch-page
 (fn [{:keys [db]} [_ url keys]]
   {:fx [[:dispatch
          [:comments/fetch url
           [:comments/load-page keys] [:bad-response]]]]
    :db (-> db
            (assoc-in (into keys [:show-comments-loading]) true)
            (assoc-in (into keys [:show-comments]) true))}))

(rf/reg-event-db
 :comments/toggle-replies
 (fn [db [_ comment-id]]
   (update-in
    db
    (into (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
          [:comments-page :comments])
    (fn [comments]
      (map #(if (= (:id %) comment-id)
              (assoc % :show-replies (not (:show-replies %)))
              %)
           comments)))))

(rf/reg-event-db
 :comments/load-paginated
 (fn [db [_ {:keys [body]}]]
   (->
     db
     (update-in
      (into (if (:main-player/show db) [:queue (:queue/position db)] [:stream])
            [:comments-page :comments])
      #(into (into [] %1) (into [] %2))
      (-> body
          (utils/apply-avatars-quality db :comments)
          :comments))
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
