(ns tubo.handlers.auth
  (:require
   [buddy.auth.backends.token :refer [token-backend]]
   [buddy.hashers :as bh]
   [ring.util.http-response :refer [bad-request internal-server-error ok]]
   [tubo.models.playlist :as playlist]
   [tubo.models.user :as user]
   [tubo.config :as config]))

(defn authenticate-token
  [{:keys [datasource]} token]
  (user/get-user-by-session token datasource))

(def backend
  (token-backend {:authfn authenticate-token}))

(defn create-register-handler
  [{:keys [datasource body-params]}]
  (if (config/get-in [:backend :registrations?])
    (if (user/get-user-by-username (:username body-params) datasource)
      (bad-request "User with that username already exists")
      (ok (user/create-user body-params datasource)))
    (bad-request "Instance has disabled registrations")))

(defn create-logout-handler
  [{:keys [identity datasource]}]
  (if (user/invalidate-user-session-id (:session-id identity) datasource)
    (ok)
    (internal-server-error "There was a problem logging out")))

(defn verify-password
  [user password]
  (and user (:valid (bh/verify password (:password user)))))

(defn create-login-handler
  [{:keys [datasource body-params]}]
  (let [user (user/get-user-by-username (:username body-params) datasource)]
    (if (verify-password user (:password body-params))
      (ok (dissoc user :password))
      (bad-request "Invalid credentials"))))

(defn create-password-reset-handler
  [{:keys [datasource body-params identity]}]
  (let [user (user/get-user-by-session (:session-id identity) datasource)]
    (if (verify-password user (:current-password body-params))
      (ok (user/update-user-password datasource
                                     (:id user)
                                     (:new-password body-params)))
      (bad-request "There was a problem updating the user password"))))

(defn create-delete-user-handler
  [{:keys [datasource body-params identity]}]
  (let [user (user/get-user-by-session (:session-id identity) datasource)]
    (if (verify-password user (:password body-params))
      (do
        (playlist/delete-owner-playlists datasource (:id user))
        (ok (user/delete-user-by-id datasource (:id user))))
      (bad-request "There was a problem removing the user"))))
