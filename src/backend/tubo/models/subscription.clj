(ns tubo.models.subscription
  (:require
   [tubo.db :as db]
   [tubo.models.channel :as channel]
   [tubo.handlers.utils :as utils]))

(defn get-subscription-channel-by-id
  [ds id]
  (db/execute-one! ds
                   {:select [:channels.*]
                    :from   [:subscriptions]
                    :join   [:channels
                             [:= :subscriptions.channel_id :channels.id]]
                    :where  [:= :subscriptions.channel_id id]}))

(defn get-subscriptions-by-user
  [{:keys [datasource identity] :as req}]
  (into
   []
   (map #(let [channel (get-subscription-channel-by-id datasource
                                                       (:channel_id %))]
           (assoc channel :avatar (utils/proxy-image (:avatar channel) req))))
   (db/plan datasource
            {:select [:*]
             :from   [:subscriptions]
             :where  [:= :user_id (:id identity)]})))

(defn add-subscriptions
  [ds values]
  (db/execute! ds
               {:insert-into [:subscriptions]
                :columns     [:user_id :channel_id]
                :values      values}))

(defn delete-subscription-by-url
  [ds url id]
  (when-let [channel (channel/get-channel-by-url url ds)]
    (db/execute! ds
                 {:delete-from [:subscriptions]
                  :where       [:and
                                [:= :channel_id (:id channel)]
                                [:= :user_id id]]})))

(defn delete-subscriptions-by-user
  [ds id]
  (db/execute! ds
               {:delete-from [:subscriptions]
                :where       [:and [:= :user_id id]]}))
