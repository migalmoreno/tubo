(ns tubo.navigation.events
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

(rf/reg-fx
 :history-go!
 (fn [idx]
   (.go js/window.history idx)))

(rf/reg-event-fx
 :navigation/history-go
 (fn [_ [_ idx]]
   {:history-go! idx}))

(rf/reg-fx
 :navigate!
 (fn [{:keys [name params query]}]
   (rfe/push-state name params query)))

(rf/reg-event-fx
 :navigation/navigate
 (fn [_ [_ route]]
   {:navigate! route}))

(rf/reg-event-fx
 :navigation/toggle-mobile-menu
 (fn [{:keys [db]} _]
   {:db            (assoc db
                          :navigation/show-mobile-menu
                          (not (:navigation/show-mobile-menu db)))
    :body-overflow (not (:navigation/show-mobile-menu db))}))

(rf/reg-event-fx
 :navigation/navigated
 (fn [{:keys [db]} [_ new-match]]
   (let [old-match   (:navigation/current-match db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)
         match       (assoc new-match :controllers controllers)]
     {:db            (-> db
                         (assoc :navigation/current-match match)
                         (assoc :navigation/show-mobile-menu false)
                         (assoc :layout/show-pagination-loading false))
      :scroll-to-top nil
      :body-overflow false
      :fx            [(when (:main-player/show db)
                        [:dispatch [:bg-player/switch-from-main]])
                      [:dispatch [:queue/show false]]
                      [:dispatch
                       [:services/fetch-all
                        [:services/load] [:bad-response]]]
                      [:dispatch
                       [:kiosks/fetch-all (:service-id db)
                        [:kiosks/load] [:bad-response]]]]})))