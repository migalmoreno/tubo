(ns tubo.middleware
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [reitit.ring.middleware.exception :as exception]
   [ring.middleware.reload :as reload]
   [ring.util.http-response :as res]
   [tubo.handlers.auth :as auth]))

(defn auth
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (res/unauthorized {:error "Not authorized"}))))

(defn wrap-token-auth
  [handler]
  (wrap-authentication handler auth/backend))

(defn reloading-ring-handler
  [f]
  (let [reload! (#'reload/reloader ["src"] true)]
    (fn
      ([request]
       (reload!)
       ((f) request))
      ([request respond raise]
       (reload!)
       ((f) request respond raise)))))

(def exception-middleware
  (exception/create-exception-middleware
   (merge exception/default-handlers
          {::exception/default
           (fn [ex _]
             (log/error ex)
             (res/internal-server-error
              {:message (ex-message ex)
               :trace   (->> ex
                             (.getStackTrace)
                             (interpose "\n")
                             (apply str))}))})))

(defn wrap-datasource
  [handler datasource]
  (fn [request]
    (handler (assoc request :datasource datasource))))

(defn wrap-cors
  [handler]
  (fn [request]
    (let [handled-request (handler request)
          headers         (:headers handled-request)
          cors-headers    (reduce-kv
                           (fn [m k v]
                             (when-not (contains? headers (str/lower-case k))
                               (assoc m k v)))
                           {}
                           {"Access-Control-Allow-Methods" "*"
                            "Access-Control-Allow-Headers" "Authorization, *"
                            "Access-Control-Allow-Origin"  "*"
                            "Access-Control-Max-Age"       "86400"})]
      (-> handled-request
          (update :headers #(merge % cors-headers))))))
