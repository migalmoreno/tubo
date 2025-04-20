(ns tubo.auth.views
  (:require
   [fork.re-frame :as fork]
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]
   [vlad.core :as vlad]))

(def signup-form-validation
  (fn [password]
    (vlad/join
     (vlad/attr ["username"] (vlad/chain (vlad/present) (vlad/length-in 5 20)))
     (vlad/attr ["password"] (vlad/chain (vlad/present) (vlad/length-over 7)))
     (vlad/attr ["confirm-password"]
                (vlad/chain
                 (vlad/equals-value
                  password
                  {:message
                   "Confirm password must be the same as password"}))))))

(defn error-field
  [error]
  (when error
    [:div.bg-red-500.p-2.rounded error]))

(defn signup
  []
  [fork/form
   {:path              [:signup-form]
    :validation        #(vlad/field-errors (signup-form-validation
                                            (get % "password"))
                                           %)
    :prevent-default?  true?
    :clean-on-unmount? true?
    :on-submit         #(rf/dispatch [:auth/signup %])}
   (fn [{:keys [values handle-change handle-blur handle-submit errors touched
                submitting?]}]
     [layout/content-container
      [:div.py-6
       [layout/content-header "Sign Up"]]
      [:form {:on-submit handle-submit}
       [layout/form-field {:label "Username"}
        [layout/input
         :name "username"
         :value (values "username")
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "username"]
        (when (touched "username")
          [error-field (first (get errors (list "username")))])]
       [layout/form-field {:label "Password"}
        [layout/input
         :name "password"
         :value (values "password")
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "password"
         :type "password"]
        (when (touched "password")
          [error-field (first (get errors (list "password")))])]
       [layout/form-field {:label "Confirm Password"}
        [layout/input
         :name "confirm-password"
         :value (values "confirm-password")
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "confirm password"
         :type "password"]
        (when (touched "confirm-password")
          [error-field (first (get errors (list "confirm-password")))])]
       [:div.flex.justify-center.py-4
        [layout/primary-button (if submitting? "Loading..." "Register") nil nil
         (when submitting? [layout/loading-icon]) {:disabled submitting?}]]]])])

(def login-form-validation
  (vlad/join
   (vlad/attr ["username"] (vlad/chain (vlad/present) (vlad/length-in 5 20)))
   (vlad/attr ["password"] (vlad/chain (vlad/present) (vlad/length-over 7)))))

(defn login
  []
  [fork/form
   {:path              [:login-form]
    :validation        #(vlad/field-errors login-form-validation %)
    :prevent-default?  true?
    :clean-on-unmount? true?
    :on-submit         #(rf/dispatch [:auth/login %])}
   (fn [{:keys [values handle-change handle-blur handle-submit errors touched
                submitting?]}]
     [layout/content-container
      [:div.py-6
       [layout/content-header "Login"]]
      [:form {:on-submit handle-submit}
       [layout/form-field {:label "Username"}
        [layout/input
         :name "username"
         :value (values "username")
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "username"]
        (when (touched "username")
          [error-field (first (get errors (list "username")))])]
       [layout/form-field {:label "Password"}
        [layout/input
         :name "password"
         :value (values "password")
         :on-change handle-change
         :on-blur handle-blur
         :placeholder "password"
         :type "password"]
        (when (touched "password")
          [error-field (first (get errors (list "password")))])]
       [:div.flex.justify-center.py-4
        [layout/primary-button (if submitting? "Loading..." "Login") nil nil
         (when submitting? [layout/loading-icon]) {:disabled submitting?}]]]])])
