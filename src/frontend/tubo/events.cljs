(ns tubo.events
  (:require
   [fork.re-frame :as fork]
   [re-frame.core :as rf]
   [re-promise.core]
   [superstructor.re-frame.fetch-fx]
   [tubo.auth.events]
   [tubo.bg-player.events]
   [tubo.bookmarks.events]
   [tubo.channel.events]
   [tubo.comments.events]
   [tubo.feed.events]
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
   [tubo.schemas :as s]
   [tubo.search.events]
   [tubo.services.events]
   [tubo.settings.events]
   [tubo.storage]
   [tubo.stream.events]
   [tubo.subscriptions.events]))

(rf/reg-event-fx
 :initialize
 (fn []
   {:db s/default-local-db
    :fx [[:fetch-store
          {:on-success [:load-db] :on-failure [:notifications/error]}]]}))

(rf/reg-event-fx
 :load-db
 (fn [{:keys [db]} [_ store]]
   (let [app-db (merge db store)]
     (merge (if (s/local-db-valid? app-db)
              {:db app-db}
              {:clear-store app-db})
            {:fx [[:dispatch
                   [:services/fetch-all
                    [:services/load] [:bad-response]]]
                  [:dispatch
                   [:kiosks/fetch-all (or (:service-id store) 0)
                    [:kiosks/load] [:bad-response]]]
                  [:dispatch
                   [:api/get "services/3/instance"
                    [:peertube/load-active-instance]
                    [:bad-response]]]
                  (when (or (and (seq (:queue app-db))
                                 (not (:main-player/show app-db)))
                            (:main-player/show app-db))
                    [:dispatch [:bg-player/switch-from-main]])
                  (when (:auth/user app-db)
                    [:dispatch
                     [:bookmarks/fetch-authenticated-playlists
                      [:bad-response]]])]}))))

(rf/reg-cofx
 :now
 (fn [cofx]
   (assoc cofx :now (js/Date))))

(rf/reg-fx
 :scroll-to-top!
 (fn []
   (when js/window
     (.scrollTo js/window #js {"top" 0 "behavior" "smooth"}))))

(rf/reg-event-fx
 :scroll-to-top
 (fn []
   {:scroll-to-top! nil}))

(rf/reg-fx
 :body-overflow
 (fn [active]
   (set! (.. js/document.body -style -overflow) (if active "hidden" "auto"))))

(rf/reg-fx
 :document-title
 (fn [title]
   (set! (.-title js/document) (str (when title (str title " - ")) "Tubo"))))

(rf/reg-fx

(rf/reg-event-fx

(rf/reg-fx
 :start-loading!
 (fn [top-loading-bar]
   (when @top-loading-bar
     (.continuousStart ^js @top-loading-bar))))

(rf/reg-event-fx
 :start-loading
 [(rf/inject-cofx ::inject/sub [:top-loading-bar])]
 (fn [{:keys [top-loading-bar]} _]
   {:start-loading! top-loading-bar}))

(rf/reg-fx
 :stop-loading!
 (fn [top-loading-bar]
   (when @top-loading-bar
     (.complete @top-loading-bar))))

(rf/reg-event-fx
 :stop-loading
 [(rf/inject-cofx ::inject/sub [:top-loading-bar])]
 (fn [{:keys [top-loading-bar]} _]
   {:stop-loading! top-loading-bar}))

(rf/reg-event-fx
 :on-success
 [(rf/inject-cofx ::inject/sub [:top-loading-bar])]
 (fn [{:keys [top-loading-bar]} [_ on-success body]]
   {:fx            [[:dispatch (conj on-success body)]]
    :stop-loading! top-loading-bar}))

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
     :credentials            :omit
     :on-success             on-success
     :on-failure             on-failure}}))

(rf/reg-event-fx
 :api/get-auth
 (fn [{:keys [db]} [_ path on-success on-failure params]]
   {:fetch
    {:method                 :get
     :url                    (str (get-in db [:settings :auth-instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :request-content-type   :json
     :response-content-types {#"application/.*json" :json}
     :headers                {"Authorization" (str "Token "
                                                   (:session-id (:auth/user
                                                                 db)))}
     :mode                   :cors
     :credentials            :omit
     :on-success             on-success
     :on-failure             on-failure}}))

(rf/reg-event-fx
 :api/post
 (fn [{:keys [db]} [_ path body on-success on-failure params]]
   {:fetch
    {:method                 :post
     :url                    (str (get-in db [:settings :instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :body                   body
     :request-content-type   :json
     :mode                   :cors
     :credentials            :omit
     :response-content-types {#"application/.*json" :json}
     :on-success             on-success
     :on-failure             on-failure}}))

(rf/reg-event-fx
 :api/post-auth
 (fn [{:keys [db]} [_ path body on-success on-failure params]]
   {:fetch
    {:method                 :post
     :url                    (str (get-in db [:settings :auth-instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :body                   body
     :request-content-type   :json
     :mode                   :cors
     :credentials            :omit
     :response-content-types {#"application/.*json" :json}
     :headers                {"Authorization" (str "Token "
                                                   (:session-id (:auth/user
                                                                 db)))}
     :on-success             on-success
     :on-failure             on-failure}}))

(rf/reg-event-fx
 :api/put-auth
 (fn [{:keys [db]} [_ path body on-success on-failure params]]
   {:fetch
    {:method                 :put
     :url                    (str (get-in db [:settings :auth-instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :body                   body
     :request-content-type   :json
     :mode                   :cors
     :credentials            :omit
     :response-content-types {#"application/.*json" :json}
     :headers                {"Authorization" (str "Token "
                                                   (:session-id (:auth/user
                                                                 db)))}
     :on-success             on-success
     :on-failure             on-failure}}))

(rf/reg-event-fx
 :api/delete-auth
 (fn [{:keys [db]} [_ path on-success on-failure params]]
   {:fetch
    {:method                 :delete
     :url                    (str (get-in db [:settings :auth-instance])
                                  "/api/v1/"
                                  path)
     :params                 (or params {})
     :request-content-type   :json
     :mode                   :cors
     :credentials            :omit
     :response-content-types {#"application/.*json" :json}
     :headers                {"Authorization" (str "Token "
                                                   (:session-id (:auth/user
                                                                 db)))}
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
   {:fx [[:dispatch [:notifications/add (assoc res :type :error)]]
         [:dispatch [:stop-loading]]]}))

(rf/reg-event-fx
 :bad-pagination-response
 (fn [{:keys [db]} [_ res]]
   {:fx [[:dispatch [:bad-response res]]]
    :db (assoc db :show-pagination-loading false)}))

(rf/reg-event-fx
 :bad-page-response
 (fn [_ [_ reload-cb res]]
   {:fx [[:dispatch
          [:change-view
           #(layout/error (assoc res :type :error) reload-cb)]]
         [:dispatch [:stop-loading]]]}))

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

(rf/reg-event-fx
 :copy-to-clipboard
 (fn [_ [_ text]]
   {:promise {:call       #(.. js/navigator -clipboard (writeText text))
              :on-success [:notifications/success "Copied to clipboard"]
              :on-failure [:notifications/error
                           "There was an error copying to clipboard"]}}))

(rf/reg-event-fx
 :on-form-submit-failure
 (fn [{:keys [db]} [_ path res]]
   {:db (fork/set-submitting db path false)
    :fx [[:dispatch [:bad-response res]]]}))
