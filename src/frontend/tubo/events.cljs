(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [day8.re-frame.http-fx]
   [nano-id.core :refer [nano-id]]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-promise.core]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [tubo.bookmarks.events]
   [tubo.channel.events]
   [tubo.comments.events]
   [tubo.kiosks.events]
   [tubo.modals.events]
   [tubo.notifications.events]
   [tubo.player.events]
   [tubo.playlist.events]
   [tubo.queue.events]
   [tubo.search.events]
   [tubo.services.events]
   [tubo.settings.events]
   [tubo.stream.events]))

(reg-co-fx! :tubo {:fx :store :cofx :store})

(rf/reg-event-fx
 :initialize-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [store]} _]
   (let [if-nil #(if (nil? %1) %2 %1)]
     {:db
      {:paused                 true
       :muted                  (:muted store)
       :queue                  (if-nil (:queue store) [])
       :service-id             (if-nil (:service-id store) 0)
       :loop-playback          (if-nil (:loop-playback store) :playlist)
       :queue-pos              (if-nil (:queue-pos store) 0)
       :volume-level           (if-nil (:volume-level store) 100)
       :background-player/show (:background-player/show store)
       :bookmarks
       (if-nil (:bookmarks store) [{:id (nano-id) :name "Liked Streams"}])
       :settings
       {:theme            (if-nil (:theme store) "auto")
        :show-comments    (if-nil (:show-comments store) true)
        :show-related     (if-nil (:show-related store) true)
        :show-description (if-nil (:show-description store) true)
        :default-service  (if-nil (:default-service store)
                            {:service-id       0
                             :id               "YouTube"
                             :default-kiosk    "Trending"
                             :available-kiosks ["Trending"]})}}})))

(rf/reg-fx
 :scroll-to-top
 (fn [_]
   (.scrollTo js/window #js {"top" 0 "behavior" "smooth"})))

(rf/reg-fx
 :body-overflow
 (fn [active]
   (set! (.. js/document.body -style -overflow) (if active "hidden" "auto"))))

(rf/reg-fx
 :document-title
 (fn [title]
   (set! (.-title js/document) (str title " - Tubo"))))

(rf/reg-fx
 :scroll-into-view!
 (fn [element]
   (when element
     (.scrollIntoView element (js-obj "behavior" "smooth")))))

(rf/reg-event-fx
 :scroll-into-view
 (fn [{:keys [db]} [_ element]]
   {:scroll-into-view! element}))

(rf/reg-fx
 :history-go!
 (fn [idx]
   (.go js/window.history idx)))

(rf/reg-event-fx
 :history-go
 (fn [_ [_ idx]]
   {:history-go! idx}))

(rf/reg-fx
 :navigate!
 (fn [{:keys [name params query]}]
   (rfe/push-state name params query)))

(rf/reg-event-fx
 :navigate
 (fn [_ [_ route]]
   {:navigate! route}))

(rf/reg-event-fx
 :toggle-mobile-nav
 (fn [{:keys [db]} _]
   {:db            (assoc db :show-mobile-nav (not (:show-mobile-nav db)))
    :body-overflow (not (:show-mobile-nav db))}))

(rf/reg-event-fx
 :navigated
 (fn [{:keys [db]} [_ new-match]]
   (let [old-match   (:current-match db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)
         match       (assoc new-match :controllers controllers)]
     {:db            (-> db
                         (assoc :current-match match)
                         (assoc :show-mobile-nav false)
                         (assoc :show-pagination-loading false))
      :scroll-to-top nil
      :body-overflow false
      :fx            [(when (:main-player/show db)
                        [:dispatch [:player/switch-from-main]])
                      [:dispatch [:queue/show false]]
                      [:dispatch [:services/fetch-all
                                  [:services/load] [:bad-response]]]
                      [:dispatch [:kiosks/fetch-all (:service-id db)
                                  [:kiosks/load] [:bad-response]]]]})))

(defonce timeouts! (r/atom {}))

(rf/reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts! id)]
     (js/clearTimeout existing)
     (swap! timeouts! dissoc id))
   (when (some? event)
     (swap! timeouts! assoc id
            (js/setTimeout #(rf/dispatch event) time)))))

(rf/reg-event-fx
 :bad-response
 (fn [{:keys [db]} [_ res]]
   {:fx [[:dispatch [:notifications/add res]]]}))

(rf/reg-fx
 :file-download
 (fn [{:keys [data name mime-type]}]
   (let [file  (.createObjectURL js/URL (js/Blob. (array data) {:type mime-type}))
         !link (.createElement js/document "a")]
     (set! (.-href !link) file)
     (set! (.-download !link) name)
     (.click !link)
     (.remove !link))))

(rf/reg-event-fx
 :load-homepage
 (fn [{:keys [db]} [_ res]]
   (let [updated-db (assoc db :services (js->clj res :keywordize-keys true))
         service-id (:id (first
                          (filter #(= (-> db :settings :default-service :id)
                                      (-> % :info :name))
                                  (:services updated-db))))]
     {:fx [[:dispatch [:kiosks/fetch-default-page service-id]]
           [:dispatch [:services/change-id service-id]]]})))

(rf/reg-event-fx
 :fetch-homepage
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [:services/fetch-all [:load-homepage] [:bad-response]]]]}))

(rf/reg-event-fx
 :change-view
 (fn [{:keys [db]} [_ view]]
   {:db (assoc-in db [:current-match :data :view] view)}))
