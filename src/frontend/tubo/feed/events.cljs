(ns tubo.feed.events
  (:require
   [re-frame.core :as rf]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :feed/load-items
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc
         db
         (if (:auth/user db) :user/feed :feed)
         (-> body
             (utils/apply-thumbnails-quality db :items)
             (utils/apply-avatars-quality db :items)))}))

(rf/reg-event-fx
 :feed/fetch
 [(rf/inject-cofx :now) (show-loading-status :api/get-auth)]
 (fn [{:keys [db now]}]
   {:db (assoc db
               (if (:auth/user db) :user/feed-last-updated :feed-last-updated)
               now)
    :fx (if (:auth/user db)
          [[:dispatch
            [:api/get-auth "user/feed"
             [:feed/load-items]
             [:bad-page-response [:auth/redirect-login]]]]]
          (when (seq (:subscriptions db))
            [[:dispatch
              [:api/get "feed" [:feed/load-items] [:bad-page-response]
               {:channels (.stringify js/JSON
                                      (clj->js (map :url
                                                    (:subscriptions
                                                     db))))}]]]))}))

(rf/reg-event-fx
 :feed/fetch-page
 (fn [{:keys [db]}]
   (let [feed (get db (if (:auth/user db) :user/feed :feed))
         subs (get db (if (:auth/user db) :user/subscriptions :subscriptions))]
     {:document-title "Feed"
      :fx             (if (and (seq feed)
                               (seq subs)
                               (= (set (map :url subs)) (set (:channels feed))))
                        []
                        [[:dispatch [:feed/fetch]]])})))
