(ns tubo.middleware
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.middleware :refer [wrap-authentication]]
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

(defn wrap-cors
  [handler]
  (fn [request]
    ((comp
      #(res/header % "Access-Control-Allow-Methods" "*")
      #(res/header % "Access-Control-Allow-Headers" "Authorization, *")
      #(res/header % "Access-Control-Allow-Origin" "*")
      #(res/header % "Access-Control-Max-Age" "86400")
      handler)
     request)))
