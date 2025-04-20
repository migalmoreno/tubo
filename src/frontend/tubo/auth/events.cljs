(ns tubo.auth.events
  (:require
   [fork.re-frame :as fork]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :auth/handle-signup-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/handle-signup-success
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:notifications/success "Registration successful"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/signup
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post "signup" values
           [:auth/handle-signup-success path]
           [:auth/handle-signup-failure path]]]]}))

(rf/reg-event-fx
 :auth/handle-logout-success
 (fn [{:keys [db]}]
   {:db (assoc db :auth/user nil)
    :fx [[:dispatch [:notifications/clear]]
         [:dispatch [:notifications/success "Logged out"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/handle-logout-failure
 (fn [_ [_ res]]
   {:fx [[:dispatch [:notifications/clear]]
         [:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/logout
 (fn []
   {:fx [[:dispatch
          [:notifications/add
           {:status-text "Logging out"
            :type        :loading}
           false]]
         [:dispatch
          [:api/post "logout" nil
           [:auth/handle-logout-success]
           [:auth/handle-logout-failure]]]]}))

(rf/reg-event-fx
 :auth/handle-login-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/handle-login-success
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:notifications/success "Login successful"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/login
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post "login" values
           [:auth/handle-login-success path]
           [:auth/handle-login-failure path]]]]}))
