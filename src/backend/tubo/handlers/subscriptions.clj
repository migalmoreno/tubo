(ns tubo.handlers.subscriptions
  (:require
   [ring.util.http-response :refer [ok]]
   [tubo.models.channel :as channel]
   [tubo.models.subscription :as subscription]
   [tubo.handlers.utils :as utils]))

(defn create-get-subscriptions-handler
  [req]
  (ok (subscription/get-subscriptions-by-user req)))

(defn create-post-subscriptions-handler
  [{:keys [datasource identity body-params]}]
  (let [channel (or (channel/get-channel-by-url (:url body-params) datasource)
                    (first (channel/add-channels
                            [[(:url body-params)
                              (:name body-params)
                              (utils/unproxy-image (:avatar body-params))
                              (:verified body-params)]]
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
