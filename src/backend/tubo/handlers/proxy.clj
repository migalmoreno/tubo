(ns tubo.handlers.proxy
  (:require
   [org.httpkit.client :as client]
   [ring.util.codec :refer [url-decode]]))

(defn create-proxy-handler
  [{:keys [request-method headers body path-params]}]
  (let [url         (url-decode (:url path-params))
        request     {:method  request-method
                     :url     url
                     :headers (dissoc headers "host")
                     :body    body}
        response    @(client/request request)
        res-headers (reduce-kv (fn [m k v]
                                 (when-not (contains? m k)
                                   (assoc m (name k) [v])))
                               {}
                               (:headers response))]
    (assoc (select-keys response [:status :body])
           :headers
           res-headers)))
