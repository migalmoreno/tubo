(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [day8.re-frame.http-fx]
   [nano-id.core :refer [nano-id]]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-promise.core]
   [tubo.bg-player.events]
   [tubo.bookmarks.events]
   [tubo.channel.events]
   [tubo.comments.events]
   [tubo.kiosks.events]
   [tubo.main-player.events]
   [tubo.modals.events]
   [tubo.navigation.events]
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
      {:player/paused  true
       :player/muted   (:player/muted store)
       :queue          (if-nil (:queue store) [])
       :service-id     (if-nil (:service-id store) 0)
       :player/loop    (if-nil (:player/loop store) :playlist)
       :queue/position (if-nil (:palyer/position store) 0)
       :player/volume  (if-nil (:player/volume store) 100)
       :bg-player/show (:bg-player/show store)
       :bookmarks      (if-nil (:bookmarks store)
                               [{:id (nano-id) :name "Liked Streams"}])
       :settings       {:theme            (if-nil (:theme store) "auto")
                        :show-comments    (if-nil (:show-comments store)
                                                  true)
                        :show-related     (if-nil (:show-related store)
                                                  true)
                        :show-description (if-nil (:show-description
                                                   store)
                                                  true)
                        :items-layout     "list"
                        :default-service  (if-nil
                                           (:default-service store)
                                           {:service-id 0
                                            :id "YouTube"
                                            :default-kiosk "Trending"
                                            :available-kiosks
                                            ["Trending"]})}}})))

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
 (fn [_ [_ element]]
   {:scroll-into-view! element}))

(defonce timeouts! (r/atom {}))

(rf/reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts! id)]
     (js/clearTimeout existing)
     (swap! timeouts! dissoc id))
   (when (some? event)
     (swap! timeouts! assoc
       id
       (js/setTimeout #(rf/dispatch event) time)))))

(rf/reg-event-fx
 :bad-response
 (fn [_ [_ res]]
   {:fx [[:dispatch [:notifications/add res]]]}))

(rf/reg-fx
 :file-download
 (fn [{:keys [data name mime-type]}]
   (let [file  (.createObjectURL js/URL
                                 (js/Blob. (array data) {:type mime-type}))
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
                          (filter #(= (-> db
                                          :settings
                                          :default-service
                                          :id)
                                      (-> %
                                          :info
                                          :name))
                                  (:services updated-db))))]
     {:fx [[:dispatch [:kiosks/fetch-default-page service-id]]
           [:dispatch [:services/change-id service-id]]]})))

(rf/reg-event-fx
 :fetch-homepage
 (fn [_ _]
   {:fx [[:dispatch [:services/fetch-all [:load-homepage] [:bad-response]]]]}))

(rf/reg-event-fx
 :change-view
 (fn [{:keys [db]} [_ view]]
   {:db (assoc-in db [:navigation/current-match :data :view] view)}))
