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
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ path {:keys [body]}]]
   {:db    (-> db
               (fork/set-submitting path false)
               (assoc :auth/user body))
    :store (assoc store :auth/user body)
    :fx    [[:dispatch [:notifications/success "Registration successful"]]
            [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/register
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post "register" values
           [:auth/handle-signup-success path]
           [:auth/handle-signup-failure path]]]]}))

(rf/reg-event-fx
 :auth/handle-invalidate-session-success
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]}]
   {:db    (assoc db :auth/user nil)
    :store (assoc store :auth/user nil)
    :fx    [[:dispatch [:notifications/clear]]
            [:dispatch [:notifications/success "Logged out"]]
            [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/handle-invalidate-session-failure
 (fn [_ [_ res]]
   {:fx [[:dispatch [:notifications/clear]]
         [:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/logout
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]}]
   {:db    (assoc db :auth/user nil)
    :store (assoc store :auth/user nil)
    :fx    [[:dispatch [:notifications/clear]]
            [:dispatch [:notifications/success "Logged out"]]
            [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/redirect-login
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]}]
   {:db    (assoc db :auth/user nil)
    :store (assoc store :auth/user nil)
    :fx    [[:dispatch [:notifications/success "Logged out"]]
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
          [:api/post "logout" nil
           [:auth/handle-invalidate-session-success]
           [:auth/handle-invalidate-session-failure]]]]}))

(rf/reg-event-fx
 :auth/handle-login-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))

(rf/reg-event-fx
 :auth/handle-login-success
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ path {:keys [body]}]]
   {:db    (-> db
               (fork/set-submitting path false)
               (assoc :auth/user body))
    :store (assoc store :auth/user body)
    :fx    [[:dispatch [:bookmarks/fetch-authenticated-playlists]]
            [:dispatch [:notifications/success "Login successful"]]
            [:dispatch [:navigation/navigate {:name :homepage}]]]}))

(rf/reg-event-fx
 :auth/login
 (fn [{:keys [db]} [_ {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/post "login" values
           [:auth/handle-login-success path]
           [:auth/handle-login-failure path]]]]}))
