(ns tubo.auth.handlers
  (:require
   [buddy.hashers :as bh]
   [ring.util.http-response :refer [bad-request internal-server-error ok]]
   [tubo.auth.queries :as auth]
   [tubo.middleware :as middleware]
   [tubo.playlists.queries :as playlists]
   [tubo.schemas :as s]))

(defn create-register-handler
  [{:keys [config datasource body-params]}]
  (if (:registrations? config)
    (if (auth/get-user-by-username (:username body-params) datasource)
      (bad-request "User with that username already exists")
      (ok (auth/create-user body-params datasource)))
    (bad-request "Instance has disabled registrations")))

(defn create-logout-handler
  [{:keys [identity datasource]}]
  (if (auth/invalidate-user-session-id (:session-id identity) datasource)
    (ok)
    (internal-server-error "There was a problem logging out")))

(defn verify-password
  [user password]
  (and user (:valid (bh/verify password (:password user)))))

(defn create-login-handler
  [{:keys [datasource body-params]}]
  (let [user (auth/get-user-by-username (:username body-params) datasource)]
    (if (verify-password user (:password body-params))
      (ok (dissoc user :password))
      (bad-request "Invalid credentials"))))

(defn create-password-reset-handler
  [{:keys [datasource body-params identity]}]
  (let [user (auth/get-user-by-session (:session-id identity) datasource)]
    (if (verify-password user (:current-password body-params))
      (ok (auth/update-user-password datasource
                                     (:id user)
                                     (:new-password body-params)))
      (bad-request "There was a problem updating the user password"))))

(defn create-delete-user-handler
  [{:keys [datasource body-params identity] :as req}]
  (let [user (auth/get-user-by-session (:session-id identity) datasource)]
    (if (verify-password user (:password body-params))
      (do
        (playlists/delete-owner-playlists req (:id user))
        (ok (auth/delete-user-by-id datasource (:id user))))
      (bad-request "There was a problem removing the user"))))

(def routes
  {:api/register {:post {:summary    "registers a user"
                         :parameters {:body {:username s/ValidUsername
                                             :password s/ValidPassword}}
                         :handler    create-register-handler}}
   :api/login {:post {:summary    "logs in a user"
                      :parameters {:body {:username s/ValidUsername
                                          :password s/ValidPassword}}
                      :handler    create-login-handler}}
   :api/logout {:post {:summary    "logs out an authenticated user"
                       :handler    create-logout-handler
                       :middleware [middleware/auth]}}
   :api/delete-user
   {:post {:summary    "deletes an authenticated user"
           :middleware [middleware/auth]
           :parameters {:body {:password s/ValidPassword}}
           :handler    create-delete-user-handler}}
   :api/password-reset
   {:post {:summary    "resets the password for an authenticated user"
           :middleware [middleware/auth]
           :parameters {:body {:current-password s/ValidPassword
                               :new-password     s/ValidPassword}}
           :handler    create-password-reset-handler}}})
