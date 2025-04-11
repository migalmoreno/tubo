(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [re-promise.core]
   [superstructor.re-frame.fetch-fx]
   [tubo.bg-player.events]
   [tubo.bookmarks.events]
   [tubo.channel.events]
   [tubo.comments.events]
   [tubo.config :as config]
   [tubo.kiosks.events]
   [tubo.layout.events]
   [tubo.layout.views :as layout]
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
     {:fx [[:dispatch
            [:services/fetch-all
             [:services/load] [:bad-response]]]]
      :db
      {:player/paused    true
       :player/muted     (:player/muted store)
       :player/shuffled  (:player/shuffled store)
       :player/loop      (if-nil (:player/loop store) :playlist)
       :player/volume    (if-nil (:player/volume store) 100)
       :bg-player/show   (:bg-player/show store)
       :queue            (if-nil (:queue store) [])
       :queue/position   (if-nil (:queue/position store) 0)
       :queue/unshuffled (:queue/unshuffled store)
       :service-id       (if-nil (:service-id store) 0)
       :bookmarks        (if-nil (:bookmarks store)
                                 [{:id (nano-id) :name "Liked Streams"}])
       :settings         {:theme                (if-nil (-> store
                                                            :settings
                                                            :theme)
                                                        "auto")
                          :show-comments        (if-nil (-> store
                                                            :settings
                                                            :show-comments)
                                                        true)
                          :show-related         (if-nil (-> store
                                                            :settings
                                                            :show-related)
                                                        true)
                          :show-description     (if-nil (-> store
                                                            :settings
                                                            :show-description)
                                                        true)
                          :items-layout         (if-nil (-> store
                                                            :settings
                                                            :items-layout)
                                                        "list")
                          :default-resolution   (if-nil
                                                 (-> store
                                                     :settings
                                                     :default-resolution)
                                                 "720p")
                          :default-video-format (if-nil
                                                 (-> store
                                                     :settings
                                                     :default-video-format)
                                                 "MPEG-4")
                          :default-audio-format (if-nil
                                                 (-> store
                                                     :settings
                                                     :default-audio-format)
                                                 "m4a")
                          :instance             (if-nil (-> store
                                                            :settings
                                                            :instance)
                                                        (config/get-in
                                                         [:frontend
                                                          :backend-url]))
                          :default-country      (if-nil (-> store
                                                            :settings
                                                            :default-country)
                                                        {0 {:name
                                                            "United States"
                                                            :code "US"}})
                          :default-kiosk        (if-nil (-> store
                                                            :settings
                                                            :default-kiosk)
                                                        {0 "Trending"})
                          :default-filter       (if-nil (-> store
                                                            :settings
                                                            :default-filter)
                                                        {0 "all"})
                          :default-service      (if-nil (-> store
                                                            :settings
                                                            :default-service)
                                                        0)}}})))

(rf/reg-fx
 :scroll-to-top
 (fn []
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
 :scroll-top!
 (fn [element]
   (when element
     (set! (.-scrollTop (.-parentNode element)) (.-offsetTop element)))))

(rf/reg-event-fx
 :scroll-top
 (fn [_ [_ element]]
   {:scroll-top! element}))

(rf/reg-event-fx
 :api/get
 (fn [{:keys [db]} [_ path on-success on-failure params]]
   {:fetch
    {:method                 :get
     :url                    (str (get-in db [:settings :instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :request-content-type   :json
     :response-content-types {#"application/.*json" :json}
     :mode                   :cors
     :credentials            :same-origin
     :on-success             on-success
     :on-failure             on-failure}}))

(defonce !timeouts (atom {}))

(rf/reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @!timeouts id)]
     (js/clearTimeout existing)
     (swap! !timeouts dissoc id))
   (when (some? event)
     (swap! !timeouts assoc
       id
       (js/setTimeout #(rf/dispatch event) time)))))

(rf/reg-event-fx
 :bad-response
 (fn [_ [_ res]]
   {:fx [[:dispatch [:notifications/add (assoc res :type :error)]]]}))

(rf/reg-event-fx
 :bad-pagination-response
 (fn [{:keys [db]} [_ res]]
   {:fx [[:dispatch [:bad-response res]]]
    :db (assoc db :show-pagination-loading false)}))

(rf/reg-event-fx
 :bad-page-response
 (fn [{:keys [db]} [_ reload-cb res]]
   {:fx [[:dispatch
          [:change-view
           #(layout/error (assoc res :type :error) reload-cb)]]]
    :db (assoc db :show-page-loading false)}))

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
 :fetch-homepage
 (fn [{:keys [db]}]
   (let [service-id (-> db
                        :settings
                        :default-service)]
     {:fx [[:dispatch [:kiosks/fetch-default-page service-id]]
           [:dispatch [:services/change-id service-id]]]})))

(rf/reg-event-fx
 :change-view
 (fn [{:keys [db]} [_ view]]
   {:db (assoc-in db [:navigation/current-match :data :view] view)}))
