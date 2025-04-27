(ns tubo.handlers.auth
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.token :refer [token-backend]]
   [buddy.hashers :as bh]
   [ring.util.response :refer [response]]
   [tubo.models.user :as user]))

(defn authenticate-token
  [{:keys [datasource]} token]
  (user/get-user-by-session token datasource))

(def backend
  (token-backend {:authfn authenticate-token}))

(defn create-unauthorized-handler
  [req _]
  (if (authenticated? req)
    (-> (response "Unauthorized request")
        (assoc :status 403))
    (-> (response "Unauthenticated request")
        (assoc :status 401))))

(defn create-register-handler
  [{:keys [datasource body-params]}]
  (if (user/get-user-by-username (:username body-params) datasource)
    {:status 500
     :body   "User with that username already exists"}
    {:status 200
     :body   (user/create-user body-params datasource)}))

(defn create-logout-handler
  [{:keys [identity datasource]}]
  (if (user/invalidate-user-session-id (:session_id identity) datasource)
    {:status 200}
    {:status 500
     :body   "There was a problem logging out"}))

(defn create-login-handler
  [{:keys [datasource body-params]}]
  (let [user (user/get-user-by-username (:username body-params) datasource)]
    (if (and user (:valid (bh/verify (:password body-params) (:password user))))
      {:status 200
       :body   (dissoc user :password)}
      {:status 500
       :body   "Invalid credentials"})))
