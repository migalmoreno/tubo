(ns tubo.subscriptions.handlers
  (:require
   [ring.util.http-response :refer [ok]]
   [tubo.queries :as queries]
   [tubo.middleware :as middleware]
   [tubo.schemas :as s]
   [tubo.subscriptions.queries :as subscriptions]
   [tubo.utils :as utils]))

(defn create-get-subscriptions-handler
  [req]
  (ok (subscriptions/get-subscriptions-by-user req)))

(defn create-post-subscriptions-handler
  [{:keys [datasource identity body-params] :as req}]
  (let [channel (or (queries/get-channel-by-url (:url body-params) datasource)
                    (first (queries/add-channels
                            [[(:url body-params)
                              (:name body-params)
                              (utils/unproxy-image (:avatar body-params) req)
                              (:verified body-params)]]
                            datasource)))]
    (ok (subscriptions/add-subscriptions datasource
                                         [[(:id identity)
                                           (:id channel)]]))))

(defn create-delete-subscription-handler
  [{:keys [datasource path-params identity]}]
  (ok (subscriptions/delete-subscription-by-url datasource
                                                (:url path-params)
                                                (:id identity))))

(defn create-delete-subscriptions-handler
  [{:keys [datasource identity]}]
  (ok (subscriptions/delete-subscriptions-by-user datasource (:id identity))))

(def routes
  {:api/user-subscriptions
   {:get    {:summary    "returns all subscriptions for an authenticated user"
             :handler    create-get-subscriptions-handler
             :middleware [middleware/auth]}
    :post   {:summary    "adds a new subscription for an authenticated user"
             :handler    create-post-subscriptions-handler
             :middleware [middleware/auth]
             :parameters {:body s/SubscriptionChannel}}
    :delete {:summary    "deletes all subscriptions for an authenticated user"
             :handler    create-delete-subscriptions-handler
             :middleware [middleware/auth]}}
   :api/user-subscription
   {:delete {:summary    "deletes a subscription for an authenticated user"
             :parameters {:path {:url string?}}
             :handler    create-delete-subscription-handler
             :middleware [middleware/auth]}}})
