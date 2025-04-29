(ns tubo.auth.views
  (:require
   [tubo.layout.views :as layout]
   [tubo.modals.views :as modals]
   [tubo.schemas :as s]))

(def password-reset-validation
  [:map
   [:current-password s/ValidPassword]
   [:new-password s/ValidPassword]])

(defn password-reset-modal
  []
  [modals/modal-content "Password Reset"
   [layout/form
    {:validation  password-reset-validation
     :on-submit   [:auth/password-reset]
     :submit-text "Reset"}
    [{:name        :current-password
      :label       "Current Password"
      :placeholder "password"
      :type        :password}
     {:name        :new-password
      :label       "New Password"
      :placeholder "password"
      :type        :password}]]])

(defn user-deletion-modal
  []
  [modals/modal-content "Delete User"
   [layout/form
    {:validation  [:map [:password s/ValidPassword]]
     :on-submit   [:auth/delete-user]
     :submit-text "Delete"}
    [{:name        :password
      :label       "Password"
      :placeholder "password"
      :type        :password}]]])

(def register-form-validation
  [:and
   [:map
    [:username s/ValidUsername]
    [:password s/ValidPassword]
    [:confirm-password string?]]
   [:fn
    {:error/message "passwords don't match"
     :error/path    [:confirm-password]}
    (fn [{:keys [password confirm-password]}]
      (= password confirm-password))]])

(defn register
  []
  [layout/content-container
   [:div.py-6
    [layout/content-header "Register"]]
   [layout/form
    {:validation  register-form-validation
     :on-submit   [:auth/register]
     :submit-text "Register"}
    [{:name        :username
      :label       "Username"
      :type        :text
      :placeholder "username"}
     {:name        :password
      :label       "Password"
      :type        :password
      :placeholder "password"}
     {:name        :confirm-password
      :label       "Confirm Password"
      :type        :password
      :placeholder "confirm password"}]]])

(def login-form-validation
  [:map
   [:username s/ValidUsername]
   [:password s/ValidPassword]])

(defn login
  []
  [layout/content-container
   [:div.py-6
    [layout/content-header "Login"]]
   [layout/form
    {:validation  login-form-validation
     :on-submit   [:auth/login]
     :submit-text "Login"}
    [{:name        :username
      :label       "Username"
      :type        :text
      :placeholder "username"}
     {:name        :password
      :label       "Password"
      :type        :password
      :placeholder :password}]]])
