(ns tubo.auth.views
  (:require
   [fork.re-frame :as fork]
   [malli.core :as m]
   [malli.error :as error]
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]))

(def register-form-validation
  [:and
   [:map
    [:username [:string {:min 3 :max 20}]]
    [:password [:string {:min 6 :max 128}]]
    [:confirm-password string?]]
   [:fn
    {:error/message "passwords don't match"
     :error/path    [:confirm-password]}
    (fn [{:keys [password confirm-password]}]
      (= password confirm-password))]])

(defn register
  []
  [fork/form
   {:path              [:register-form]
    :validation        #(-> (m/explain register-form-validation %)
                            (error/humanize))
    :keywordize-keys   true
    :prevent-default?  true?
    :clean-on-unmount? true?
    :on-submit         #(rf/dispatch [:auth/register %])}
   (fn [{:keys [values handle-change handle-blur handle-submit errors touched
                submitting? normalize-name]}]
     [layout/content-container
      [:div.py-6
       [layout/content-header "Register"]]
      [:form {:on-submit handle-submit}
       [layout/form-field {:label "Username"}
        [layout/input
         :name (normalize-name :username)
         :value (values :username)
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "username"]
        (when (touched :username)
          [layout/error-field (first (:username errors))])]
       [layout/form-field {:label "Password"}
        [layout/input
         :name (normalize-name :password)
         :value (values :password)
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "password"
         :type "password"]
        (when (touched :password)
          [layout/error-field (first (:password errors))])]
       [layout/form-field {:label "Confirm Password"}
        [layout/input
         :name "confirm-password"
         :value (values :confirm-password)
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "confirm password"
         :type "password"]
        (when (touched :confirm-password)
          [layout/error-field (first (:confirm-password errors))])]
       [:div.flex.justify-center.py-4
        [layout/primary-button (if submitting? "Loading..." "Register") nil nil
         (when submitting? [layout/loading-icon]) {:disabled submitting?}]]]])])

(def login-form-validation
  [:map
   [:username string?]
   [:password string?]])

(defn login
  []
  [fork/form
   {:path              [:login-form]
    :validation        #(-> (m/explain login-form-validation %)
                            (error/humanize))
    :keywordize-keys   true
    :prevent-default?  true?
    :clean-on-unmount? true?
    :on-submit         #(rf/dispatch [:auth/login %])}
   (fn [{:keys [values handle-change handle-blur handle-submit errors touched
                submitting? normalize-name]}]
     [layout/content-container
      [:div.py-6
       [layout/content-header "Login"]]
      [:form {:on-submit handle-submit}
       [layout/form-field {:label "Username"}
        [layout/input
         :name (normalize-name :username)
         :value (values :username)
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "username"]
        (when (touched :username)
          [layout/error-field (first (:username errors))])]
       [layout/form-field {:label "Password"}
        [layout/input
         :name (normalize-name :password)
         :value (values :password)
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "password"
         :type "password"]
        (when (touched :password)
          [layout/error-field (first (:password errors))])]
       [:div.flex.justify-center.py-4.gap-x-4
        [layout/primary-button (if submitting? "Loading..." "Login") nil nil
         (when submitting? [layout/loading-icon]) {:disabled submitting?}]]]])])
