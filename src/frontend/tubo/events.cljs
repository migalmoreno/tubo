(ns tubo.events
  (:require
   ["fast-average-color" :refer [FastAverageColor]]
   ["localforage" :as localforage]
   ["motion" :refer [animate]]
   [cognitect.transit :as transit]
   [fork.re-frame :as fork]
   [malli.error :as me]
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [re-frame.db :as db]
   [re-promise.core]
   [superstructor.re-frame.fetch-fx]
   [tubo.auth.events]
   [tubo.bookmarks.events]
   [tubo.channel.events]
   [tubo.comments.events]
   [tubo.feed.events]
   [tubo.kiosks.events]
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
   [tubo.stream.events]
   [tubo.subscriptions.events]
   [tubo.ui :as ui]
   [vimsical.re-frame.cofx.inject :as inject]))

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
                    [:dispatch [:main-player/unmount]])
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
 :virtuoso-scroll-to-index
 (fn [[virtuoso idx]]
   (when virtuoso
     (.scrollToIndex virtuoso
                     #js {"index"    idx
                          "align"    "start"
                          "behavior" "smooth"}))))

(rf/reg-event-fx
 :scroll-to-index
 [(rf/inject-cofx ::inject/sub [:virtuoso])]
 (fn [{:keys [virtuoso]} [_ idx]]
   (when @virtuoso
     {:virtuoso-scroll-to-index [@virtuoso idx]})))

(rf/reg-event-fx
 :get-color-async
 (fn [_ [_ image on-success on-error]]
   {:promise {:call       #(.getColorAsync (FastAverageColor.) image)
              :on-success on-success
              :on-failure (or on-error [:noop])}}))

(rf/reg-fx
 :animate!
 (fn [[elem props opts]]
   (when elem
     (animate elem (clj->js props) (clj->js opts)))))

(rf/reg-event-fx
 :animate
 (fn [_ [_ elem props opts]]
   (when elem
     {:animate! [elem props opts]})))

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
 :noop
 (fn []))

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
 (fn [{:keys [id event time] :or {id (nano-id)}}]
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
           #(ui/error-container (assoc res :type :error) reload-cb)]]
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

(rf/reg-event-db
 :layout/show-bg-overlay
 (fn [db [_ {:keys [on-click] :as data} remain-open?]]
   (assoc db
          :layout/bg-overlay
          (assoc data
                 :show?    true
                 :on-click #(do (when on-click (on-click))
                                (when-not remain-open?
                                  (rf/dispatch [:layout/hide-bg-overlay])))))))

(rf/reg-event-db
 :layout/hide-bg-overlay
 (fn [db _]
   (assoc-in db [:layout/bg-overlay :show?] false)))

(rf/reg-event-fx
 :layout/show-mobile-tooltip
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :layout/mobile-tooltip (assoc data :show? true))
    :fx [[:dispatch
          [:layout/register-tooltip data]]
         [:dispatch [:layout/show-bg-overlay {:extra-classes ["z-40"]}]]]}))

(rf/reg-event-fx
 :layout/show-mobile-panel
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :layout/mobile-panel (assoc data :show? true))
    :fx [[:dispatch [:layout/register-panel data]]]}))

(defn default-popover-data
  []
  {:id                    (nano-id)
   :destroy-on-click-out? true})

(rf/reg-event-db
 :layout/register-panel
 (fn [db [_ data]]
   (let [full-data (merge (default-popover-data) data)]
     (assoc-in db [:layout/panels (:id data)] full-data))))

(rf/reg-event-db
 :layout/destroy-panels-by-ids
 (fn [db [_ ids]]
   (update db :layout/panels #(apply dissoc % ids))))

(rf/reg-event-db
 :layout/register-tooltip
 (fn [db [_ data]]
   (let [full-data (merge (default-popover-data) data)]
     (assoc-in db [:layout/tooltips (:id data)] full-data))))

(rf/reg-event-fx
 :layout/destroy-tooltip-by-id
 (fn [{:keys [db]} [_ id]]
   {:db (update db :layout/tooltips dissoc id)}))

(rf/reg-event-fx
 :layout/change-tooltip-items
 (fn [{:keys [db]} [_ id items]]
   {:db (update-in db [:layout/tooltips id] #(assoc %1 :items %2) items)}))

(rf/reg-event-db
 :layout/destroy-tooltips-by-ids
 (fn [db [_ ids]]
   (update db :layout/tooltips #(apply dissoc % ids))))

(rf/reg-event-fx
 :layout/destroy-tooltips-on-click-out
 (fn [{:keys [db]} [_ clicked-node]]
   (when (seq (:layout/tooltips db))
     (let [clicked-controller (ui/find-clicked-controller-id
                               clicked-node
                               ui/tooltip-class-prefix)
           tooltips-ids       (->> (:layout/tooltips db)
                                   (vals)
                                   (filter :destroy-on-click-out?)
                                   (map :id)
                                   (set))]
       {:fx [[:dispatch
              [:layout/destroy-tooltips-by-ids
               (disj tooltips-ids clicked-controller)]]]}))))

(rf/reg-event-fx
 :layout/destroy-panels-on-click-out
 (fn [{:keys [db]} [_ clicked-node]]
   (when (and (seq (:layout/panels db)) (not (seq (:layout/tooltips db))))
     (let [clicked-controller (ui/find-clicked-controller-id
                               clicked-node
                               ui/panel-class-prefix)
           panels-ids         (->> (:layout/panels db)
                                   (vals)
                                   (filter :destroy-on-click-out?)
                                   (map :id)
                                   (set))]
       {:fx [[:dispatch
              [:layout/destroy-panels-by-ids
               (disj panels-ids clicked-controller)]]]}))))

(rf/reg-fx
 :intersection-observer
 (fn [{:keys [observer elem cb opts]}]
   (when @observer
     (.disconnect @observer))
   (when elem
     (.observe
      (reset! observer (js/IntersectionObserver. cb (clj->js opts)))
      elem))))

(rf/reg-event-fx
 :layout/add-intersection-observer
 (fn [{:keys [db]} [_ observer elem cb opts]]
   (when-not (:show-pagination-loading db)
     {:intersection-observer
      {:observer observer :elem elem :cb cb :opts opts}})))

(defn json->clj
  [json]
  (transit/read (transit/reader :json) json))

(rf/reg-fx
 :persist!
 (fn []
   (let [persisted-db (select-keys @db/app-db s/persisted-local-db-keys)]
     (when-let [json (try (transit/write (transit/writer :json) persisted-db)
                          (catch :default e
                            #(rf/dispatch [:notifications/error e])))]
       (-> (localforage/setItem "tubo" json)
           (p/catch #(rf/dispatch [:notifications/error %])))))))

(rf/reg-event-fx
 :persist
 (fn []
   {:persist! nil}))

(rf/reg-fx
 :validate
 (fn [{:keys [db event]}]
   (when-not (s/local-db-valid? db)
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Local DB spec check failed: "
                            (me/humanize (s/local-db-explain db))))))))

(rf/reg-fx
 :fetch-store
 (fn [{:keys [on-success on-error on-finally]}]
   (-> (localforage/getItem "tubo")
       (p/then #(when on-success (rf/dispatch (conj on-success (json->clj %)))))
       (p/catch #(when on-error (rf/dispatch (conj on-error %))))
       (p/finally #(when on-finally (rf/dispatch on-finally))))))

(rf/reg-fx
 :clear-store
 (fn [db]
   (js/console.error (js/Error. (str "Local DB spec check failed: "
                                     (me/humanize (s/local-db-explain db)))))
   (localforage/clear)))
