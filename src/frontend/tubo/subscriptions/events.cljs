(ns tubo.subscriptions.events
  (:require
   [clojure.set :refer [rename-keys]]
   [re-frame.core :as rf]
   [tubo.storage :refer [persist]]
   [tubo.layout.events :refer [show-loading-status]]
   [tubo.utils :as utils]))

(rf/reg-event-fx
 :subscriptions/load-user-subs
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :user/subscriptions
               (map
                #(rename-keys % {:avatar :thumbnail})
                body))}))

(rf/reg-event-fx
 :subscriptions/fetch
 [(show-loading-status :api/get-auth)]
 (fn [_ [_ on-error]]
   {:fx [[:dispatch
          [:api/get-auth "user/subscriptions" [:subscriptions/load-user-subs]
           on-error]]]}))

(rf/reg-event-fx
 :subscriptions/fetch-page
 (fn [{:keys [db]}]
   (if (:auth/user db)
     {:document-title (str (:username (:auth/user db)) "'s subscriptions")
      :fx             [[:dispatch
                        [:subscriptions/fetch
                         [:bad-page-response [:auth/redirect-login]]]]]}
     {:document-title "Subscriptions"})))

(rf/reg-event-fx
 :subscriptions/on-add-auth
 (fn [{:keys [db]} [_ item]]
   {:db (update db
                :user/subscriptions
                #(into [] (conj (into [] %1) %2))
                item)
    :fx [[:dispatch
          [:notifications/success
           (str "Subscribed to \"" (:name item) "\"")]]]}))

(rf/reg-event-fx
 :subscriptions/add-channel-to-subscription
 [persist]
 (fn [{:keys [db]} [_ {:keys [body]}]]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/post-auth "user/subscriptions"
             (-> body
                 (utils/apply-image-quality db :avatar :avatars)
                 (select-keys [:url :name :avatar :verified]))
             [:subscriptions/on-add-auth body]
             [:bad-response]]]]}
     {:db (update db
                  :subscriptions
                  #(into [] (conj (into [] %1) %2))
                  (utils/apply-image-quality body db :thumbnail :avatars))
      :fx [[:dispatch
            [:notifications/success
             (str "Subscribed to \"" (:name body) "\"")]]]})))

(rf/reg-event-fx
 :subscriptions/add
 (fn [{:keys [db]} [_ item]]
   (let [subs (get db (if (:auth/user db) :user/subscriptions :subscriptions))]
     (if-not (some #(= (:url %) (:url item)) subs)
       {:fx [[:dispatch
              [:channel/fetch (:url item)
               [:subscriptions/add-channel-to-subscription]
               [:bad-response]]]]}
       {:fx [[:dispatch [:notifications/error "Already subscribed"]]]}))))

(rf/reg-event-fx
 :subscriptions/on-delete-auth
 (fn [{:keys [db]} [_ url]]
   {:db (update db
                :user/subscriptions
                (fn [subs] (filter #(not= (:url %) url) subs)))
    :fx [[:dispatch
          [:notifications/success "Unsubscribed"]]]}))

(rf/reg-event-fx
 :subscriptions/remove
 [persist]
 (fn [{:keys [db]} [_ url]]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/delete-auth
             (str "user/subscriptions/" (js/encodeURIComponent url))
             [:subscriptions/on-delete-auth url]
             [:bad-response]]]]}
     {:db (update db
                  :subscriptions
                  (fn [subs] (filter #(not= (:url %) url) subs)))
      :fx [[:dispatch
            [:notifications/success "Unsubscribed"]]]})))

(rf/reg-event-fx
 :subscriptions/on-clear-auth
 (fn [{:keys [db]}]
   {:db (assoc db
               :user/subscriptions nil
               :user/feed          nil)
    :fx [[:dispatch
          [:notifications/success "Cleared all subscriptions"]]]}))

(rf/reg-event-fx
 :subscriptions/clear
 [persist]
 (fn [{:keys [db]}]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/delete-auth "user/subscriptions"
             [:subscriptions/on-clear-auth] [:bad-response]]]]}
     {:db (assoc db
                 :subscriptions nil
                 :feed          nil)
      :fx [[:dispatch [:notifications/success "Cleared all subscriptions"]]]})))
