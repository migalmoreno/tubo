(ns tubo.handlers
  (:require
   [clojure.data.json :as json]
   [org.httpkit.client :as client]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.codec :refer [url-decode]]
   [ring.util.http-response :refer [ok]]
   [tubo.extractors.handlers :as extractor]
   [tubo.middleware :as middleware]
   [tubo.subscriptions.queries :as subscriptions]
   [tubo.utils :as utils]))

(defn get-channels-latest-streams
  [channels req]
  (->> channels
       (map #(-> (assoc-in req [:path-params :url] (:url %))
                 extractor/get-channel-tab-info
                 (utils/->ListInfo req)
                 :related-items))
       flatten
       (remove nil?)
       (sort-by :upload-date #(> %1 %2))))

(defn create-get-user-feed-handler
  [req]
  (let [channels (subscriptions/get-subscriptions-by-user req)]
    (when (seq channels)
      (ok {:items    (get-channels-latest-streams channels req)
           :channels (map :url channels)}))))

(defn create-get-feed-handler
  [{:keys [query-params] :as req}]
  (let [urls (json/read-str (get query-params "channels"))]
    (when (seq urls)
      (ok {:items    (get-channels-latest-streams (map (fn [url] {:url url})
                                                       urls)
                                                  req)
           :channels urls}))))

(defn create-proxy-handler
  [{:keys [request-method headers body path-params]}]
  (let [url         (url-decode (:url path-params))
        request     {:method  request-method
                     :url     url
                     :headers (dissoc headers "host")
                     :body    body}
        response    @(client/request request)
        res-headers (reduce-kv (fn [m k v]
                                 (cond-> m
                                   (not (contains? m k)) (assoc (name k) [v])))
                               {}
                               (:headers response))]
    (assoc (select-keys response [:status :body])
           :headers
           res-headers)))

(def routes
  {:api/feed {:get
              {:summary "returns latest streams for a list of channel URLs"
               :handler create-get-feed-handler}}
   :api/health {:no-doc true
                :get    (constantly (ok))}
   :api/proxy {:handler create-proxy-handler}
   :api/swagger-spec {:no-doc true
                      :get    {:swagger {:info     {:title "Tubo API"}
                                         :basePath "/"}
                               :handler (swagger/create-swagger-handler)}}
   :api/swagger-ui {:no-doc true
                    :get    (swagger-ui/create-swagger-ui-handler)}
   :api/user-feed
   {:get
    {:summary
     "returns latest streams for an authenticated user's subscriptions"
     :handler create-get-user-feed-handler
     :middleware [middleware/auth]}}})
