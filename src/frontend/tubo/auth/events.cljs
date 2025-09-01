(ns tubo.auth.events
  (:require
   [fork.re-frame :as fork]
   [re-frame.core :as rf]
   [tubo.storage :refer [persist]]))

(rf/reg-event-fx
 :auth/handle-signup-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/handle-signup-success
 [persist]
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:notifications/success "Registration successful"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/register
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post-auth "register" values
           [:auth/handle-signup-success path]
           [:auth/handle-signup-failure path]]]]}))

(rf/reg-event-fx
 :auth/handle-invalidate-session-success
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :auth/user nil)
    :fx [[:dispatch [:notifications/clear]]
         [:dispatch [:notifications/success "Logged out"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/handle-invalidate-session-failure
 (fn [_ [_ res]]
   {:fx [[:dispatch [:notifications/clear]]
         [:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/logout
 [persist]
 (fn [{:keys [db]}]
   {:db (-> db
            (assoc :auth/user nil)
            (assoc :user/bookmarks nil))
    :fx [[:dispatch [:notifications/clear]]
         [:dispatch [:notifications/success "Logged out"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/redirect-login
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :auth/user nil)
    :fx [[:dispatch [:notifications/success "Logged out"]]
         [:dispatch [:navigation/navigate {:name :login-page}]]]}))

(rf/reg-event-fx
 :auth/invalidate-session
 (fn []
   {:fx [[:dispatch
          [:notifications/add
           {:status-text "Logging out"
            :type        :loading}
           false]]
         [:dispatch
          [:api/post-auth "logout" nil
           [:auth/handle-invalidate-session-success]
           [:auth/handle-invalidate-session-failure]]]]}))

(rf/reg-event-fx
 :auth/handle-login-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/handle-login-success
 [persist]
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:bookmarks/fetch-authenticated-playlists]]
         [:dispatch [:notifications/success "Login successful"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/login
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post-auth "login" values
           [:auth/handle-login-success path]
           [:auth/handle-login-failure path]]]]}))

(rf/reg-event-fx
 :auth/handle-password-reset-success
 [persist]
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:modals/close]]
         [:dispatch
          [:notifications/success "Password reset"]]]}))

(rf/reg-event-fx
 :auth/password-reset
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post-auth "password-reset" values
           [:auth/handle-password-reset-success path]
           [:on-form-submit-failure path]]]]}))

(rf/reg-event-fx
 :auth/handle-delete-user-success
 [persist]
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> db
            (fork/set-submitting path false)
            (assoc :auth/user body))
    :fx [[:dispatch [:modals/close]]
         [:dispatch [:auth/logout]]
         [:dispatch [:notifications/success "Removed user"]]
         [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/delete-user
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post-auth "delete-user" values
           [:auth/handle-delete-user-success path]
           [:on-form-submit-failure path]]]]}))
