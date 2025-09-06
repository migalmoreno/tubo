(ns tubo.handlers.subscriptions
  (:require
   [ring.util.http-response :refer [ok]]
   [tubo.models.channel :as channel]
   [tubo.models.subscription :as subscription]))

(defn create-get-subscriptions-handler
  [{:keys [datasource identity]}]
  (ok (subscription/get-subscriptions-by-user datasource (:id identity))))

(defn create-post-subscriptions-handler
  [{:keys [datasource identity body-params]}]
  (let [channel (or (channel/get-channel-by-url (:url body-params) datasource)
                    (first (channel/add-channels
                            [(into []
                                   (map body-params
                                        [:url :name :avatar :verified]))]
                            datasource)))]
    (ok (subscription/add-subscriptions datasource
                                        [[(:id identity)
                                          (:id channel)]]))))

(defn create-delete-subscription-handler
  [{:keys [datasource path-params identity]}]
  (ok (subscription/delete-subscription-by-url datasource
                                               (:url path-params)
                                               (:id identity))))

(defn create-delete-subscriptions-handler
  [{:keys [datasource identity]}]
  (ok (subscription/delete-subscriptions-by-user datasource (:id identity))))
