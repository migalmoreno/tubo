(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [day8.re-frame.http-fx]
   [goog.object :as gobj]
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [tubo.api :as api]
   [tubo.components.modals.bookmarks :as bookmarks]
   [vimsical.re-frame.cofx.inject :as inject]))

(reg-co-fx! :tubo {:fx :store :cofx :store})

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [store]} _]
   (let [{:keys [theme show-comments show-related show-description
                 media-queue media-queue-pos show-audio-player
                 loop-playback volume-level muted bookmarks
                 default-service default-service-kiosk service-id]} store]
     {:db
      {:search-query      ""
       :service-id        (if (nil? service-id) 0 service-id)
       :stream            {}
       :search-results    []
       :services          []
       :paused            true
       :loop-playback     (if (nil? loop-playback) :playlist loop-playback)
       :media-queue       (if (nil? media-queue) [] media-queue)
       :media-queue-pos   (if (nil? media-queue-pos) 0 media-queue-pos)
       :volume-level      (if (nil? volume-level) 100 volume-level)
       :bookmarks         (if (nil? bookmarks)
                            [{:id    (nano-id)
                              :name  "Liked Streams"
                              :items []}]
                              bookmarks)
       :muted             (if (nil? muted) false muted)
       :current-match     nil
       :show-audio-player (if (nil? show-audio-player) false show-audio-player)
       :settings
       {:theme            (if (nil? theme) :light theme)
        :show-comments    (if (nil? show-comments) true show-comments)
        :show-related     (if (nil? show-related) true show-related)
        :show-description (if (nil? show-description) true show-description)
        :default-service  (if (nil? default-service)
                            {:service-id       0
                             :id               "YouTube"
                             :default-kiosk    "Trending"
                             :available-kiosks ["Trending"]}
                            default-service)}}})))

(rf/reg-fx
 ::scroll-to-top
 (fn [_]
   (.scrollTo js/window #js {"top" 0 "behavior" "smooth"})))

(rf/reg-fx
 ::history-go!
 (fn [idx]
   (.go js/window.history idx)))

(rf/reg-fx
 ::body-overflow!
 (fn [active]
   (set! (.. js/document.body -style -overflow) (if active "hidden" "auto"))))

(rf/reg-fx
 ::scroll-into-view!
 (fn [element]
   (when element
     (.scrollIntoView element (js-obj "behavior" "smooth")))))

(rf/reg-fx
 ::document-title!
 (fn [title]
   (set! (.-title js/document) (str title " - Tubo"))))

(rf/reg-fx
 ::player-volume
 (fn [{:keys [player volume]}]
   (when (and @player (> (.-readyState @player) 0))
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 ::player-mute
 (fn [{:keys [player muted?]}]
   (when (and @player (> (.-readyState @player) 0))
     (set! (.-muted @player) muted?))))

(rf/reg-fx
 ::player-src
 (fn [{:keys [player src current-pos]}]
   (set! (.-src @player) src)
   (set! (.-onended @player) #(rf/dispatch [::change-media-queue-pos (+ current-pos 1)]))))

(rf/reg-fx
 ::player-pause
 (fn [{:keys [paused? player]}]
   (when (and @player (> (.-readyState @player) 0))
     (if paused?
       (.play @player)
       (.pause @player)))))

(rf/reg-fx
 ::player-current-time
 (fn [{:keys [time player]}]
   (set! (.-currentTime @player) time)))

(rf/reg-event-db
 ::change-player-paused
 (fn [db [_ val]]
   (assoc db :paused val)))

(rf/reg-event-fx
 ::set-player-paused
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ paused?]]
   {::player-pause {:paused? (not paused?)
                    :player  player}}))

(rf/reg-event-fx
 ::player-start
 [(rf/inject-cofx ::inject/sub [:player]) (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db player]} _]
   {:fx [[:dispatch [::change-player-paused true]]
         [:dispatch [::set-player-paused false]]
         [::player-volume {:player player :volume (:volume-level db)}]]
    :db (assoc db :player-ready (and @player (> (.-readyState @player) 0)))}))

(rf/reg-event-fx
 ::set-player-time
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ time]]
   {::player-current-time {:time time :player player}}))

(rf/reg-fx
 ::stream-metadata
 (fn [metadata]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-metadata js/navigator.mediaSession) (js/MediaMetadata. (clj->js metadata))))))

(rf/reg-fx
 ::media-session
 (fn [{:keys [current-pos player stream]}]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [updatePositionState
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     (.-currentTime @player)})]
       (.setActionHandler js/navigator.mediaSession "play" #(.play @player))
       (.setActionHandler js/navigator.mediaSession "pause" #(.pause @player))
       (.setActionHandler js/navigator.mediaSession "previoustrack"
                          #(rf/dispatch [::change-media-queue-pos (- current-pos 1)]))
       (.setActionHandler js/navigator.mediaSession "nexttrack"
                          #(rf/dispatch [::change-media-queue-pos (+ current-pos 1)]))
       (.setActionHandler js/navigator.mediaSession "seekbackward"
                          (fn [^js/navigator.MediaSessionActionDetails details]
                            (set! (.-currentTime @player)
                                  (- (.-currentTime @player) (or (.-seekOffset details) 10)))
                            (updatePositionState)))
       (.setActionHandler js/navigator.mediaSession "seekforward"
                          (fn [^js/navigator.MediaSessionActionDetails details]
                            (set! (.-currentTime @player)
                                  (+ (.-currentTime @player) (or (.-seekOffset details) 10)))
                            (updatePositionState)))
       (.setActionHandler js/navigator.mediaSession "seekto"
                          (fn [^js/navigator.MediaSessionActionDetails details]
                            (set! (.-currentTime @player) (.-seekTime details))
                            (updatePositionState)))
       (.setActionHandler js/navigator.mediaSession "stop"
                          (fn []
                            (.pause @player)
                            (set! (.-currentTime @player) 0)))))))

(rf/reg-event-fx
 ::history-go
 (fn [_ [_ idx]]
   {::history-go! idx}))

(rf/reg-event-db
 ::show-search-form
 (fn [db [_ show?]]
   (when-not (= (-> db :current-match :path) "search")
     (assoc db :show-search-form show?))))

(rf/reg-event-fx
 ::toggle-mobile-nav
 (fn [{:keys [db]} _]
   {:db (assoc db :show-mobile-nav (not (:show-mobile-nav db)))
    ::body-overflow! (not (:show-mobile-nav db))}))

(rf/reg-event-fx
 ::show-media-queue
 (fn [{:keys [db]} [_ show?]]
   {:db (assoc db :show-media-queue show?)
    ::body-overflow! show?}))

(rf/reg-event-fx
 ::scroll-into-view
 (fn [{:keys [db]} [_ element]]
   {::scroll-into-view! element}))

(rf/reg-event-fx
 ::change-volume-level
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ value player]]
   {:db (assoc db :volume-level value)
    :store (assoc store :volume-level value)
    ::player-volume {:player player :volume value}}))

(rf/reg-event-fx
 ::toggle-mute
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ player]]
   {:db (assoc db :muted (not (:muted db)))
    :store (assoc store :muted (not (:muted store)))
    ::player-mute {:player player :muted? (not (:muted db))}}))

(rf/reg-event-fx
 ::toggle-loop-playback
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [loop-state (case (:loop-playback db)
                      :stream false
                      :playlist :stream
                      :playlist)]
     {:db    (assoc db :loop-playback loop-state)
      :store (assoc store :loop-playback loop-state)})))

(rf/reg-event-fx
 ::navigated
 (fn [{:keys [db]} [_ new-match]]
   (let [old-match (:current-match db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)
         match (assoc new-match :controllers controllers)]
     {:db (-> db
              (assoc :current-match match)
              (assoc :show-mobile-nav false)
              (assoc :show-pagination-loading false))
      ::scroll-to-top nil
      ::body-overflow! false
      :fx [[:dispatch [::show-media-queue false]]
           [:dispatch [::get-services]]
           [:dispatch [::get-kiosks (:service-id db)]]]})))

(rf/reg-event-fx
 ::navigate
 (fn [_ [_ route]]
   {::navigate! route}))

(rf/reg-fx
 ::navigate!
 (fn [{:keys [name params query]}]
   (rfe/push-state name params query)))

(rf/reg-event-db
 ::bad-response
 (fn [db [_ res]]
   (.log js/console (clj->js res))
   (assoc db :http-response (get-in res [:response :error]))))

(rf/reg-event-db
 ::change-search-query
 (fn [db [_ res]]
   (assoc db :search-query res)))

(rf/reg-event-fx
 ::change-service-id
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ service-id]]
   {:db (assoc db :service-id service-id)
    :store (assoc store :service-id service-id)}))

(rf/reg-event-db
 ::load-paginated-channel-results
 (fn [db [_ res]]
   (-> db
       (update-in [:channel :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:channel :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::channel-pagination
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/channels/" (js/encodeURIComponent uri) )
       [::load-paginated-channel-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-paginated-playlist-results
 (fn [db [_ res]]
   (-> db
       (update-in [:playlist :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:playlist :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::playlist-pagination
 (fn [{:keys [db]} [_ uri next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/playlists/" (js/encodeURIComponent uri))
       [::load-paginated-playlist-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-paginated-search-results
 (fn [db [_ res]]
   (-> db
       (update-in [:search-results :items] #(apply conj %1 %2)
                  (:items (js->clj res :keywordize-keys true)))
       (assoc-in [:search-results :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::search-pagination
 (fn [{:keys [db]} [_ query id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/services/" id "/search")
       [::load-paginated-search-results] [::bad-response]
       {:q query
        :nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-fx
 ::add-to-media-queue
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream]]
   (let [updated-db (update db :media-queue conj stream)]
     {:db    updated-db
      :store (assoc store :media-queue (:media-queue updated-db))})))

(rf/reg-event-fx
 ::remove-from-media-queue
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ pos]]
   (let [updated-db (update db :media-queue #(into (subvec % 0 pos) (subvec % (inc pos))))]
     {:db    updated-db
      :store (assoc store :media-queue (:media-queue updated-db))
      :fx    (if (and (= pos (:media-queue-pos db)) (not (= (count (:media-queue updated-db)) 0)))
               [[:dispatch [::change-media-queue-pos pos]]]
               (if (= (count (:media-queue updated-db)) 0)
                 [[:dispatch [::dispose-audio-player]]
                  [:dispatch [::show-media-queue false]]]))})))

(rf/reg-event-fx
 ::change-media-queue-pos
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ i]]
   (let [idx    (if (< i (count (:media-queue db)))
                  i
                  (when (= (:loop-playback db) :playlist) 0))
         stream (get (:media-queue db) idx)]
     (when stream
       {:db    (-> db
                   (assoc :player-ready false)
                   (assoc :media-queue-pos idx)
                   (assoc-in [:media-queue idx :stream] ""))
        :store (assoc store :media-queue-pos idx)
        :fx    [[:dispatch [::fetch-audio-player-stream (:url stream) idx true]]]}))))

(rf/reg-event-fx
 ::change-media-queue-stream
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ src idx]]
   (let [update-entry #(assoc-in % [:media-queue idx :stream] src)]
     {:db    (update-entry db)
      :store (update-entry store)})))

(rf/reg-event-fx
 ::dispose-audio-player
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [remove-entries
         (fn [elem]
           (-> elem
               (assoc :show-audio-player (not (:show-audio-player elem)))
               (assoc :player-ready false)
               (assoc :media-queue [])
               (assoc :media-queue-pos 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)
      :fx [[:dispatch [::set-player-paused true]]
           [:dispatch [::set-player-time 0]]]})))

(rf/reg-event-fx
 ::switch-to-audio-player
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream]]
   (let [updated-db (update db :media-queue conj stream)
         idx        (.indexOf (:media-queue updated-db) stream)]
     {:db    (-> updated-db
                 (assoc :show-audio-player true))
      :store (-> store
                 (assoc :show-audio-player true)
                 (assoc :media-queue (:media-queue updated-db)))
      :fx    [[:dispatch [::fetch-audio-player-stream (:url stream) idx (= (count (:media-queue db)) 0)]]]})))

(rf/reg-event-fx
 ::start-stream-radio
 (fn [{:keys [db]} [_ stream]]
   {:fx [[:dispatch [::switch-to-audio-player stream]]
         (when (not= (count (:media-queue db)) 0)
           [:dispatch [::change-media-queue-pos (count (:media-queue db))]])
         [:dispatch [::fetch-audio-player-related-streams (:url stream)]]]}))

(rf/reg-event-fx
 ::enqueue-related-streams
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ streams]]
   {:db (assoc db :show-audio-player true)
    :store (assoc store :show-audio-player true)
    :fx (into [] (conj
                  (map #(when (= (:type %) "stream")
                          (identity [:dispatch [::add-to-media-queue %]])) streams)
                  (when (= (:type (first streams)) "stream")
                    [:dispatch [::fetch-audio-player-stream (first streams)
                                (count (:media-queue db)) (= (count (:media-queue db)) 0)]])))}))

(rf/reg-event-db
 ::modal
 (fn [db [_ data]]
   (assoc db :modal data)))

(rf/reg-event-fx
 ::close-modal
 (fn [{:keys [db]} _]
   {:db (assoc db :modal {:show? false :child nil})
    ::body-overflow! false}))

(rf/reg-event-fx
 ::open-modal
 (fn [_ [_ child]]
   {:fx [[:dispatch [::modal {:show? true :child child}]]]
    ::body-overflow! true}))

(rf/reg-event-fx
 ::add-bookmark-list-modal
 (fn [_ [_ child]]
   {:fx [[:dispatch [::open-modal child]]]}))

(rf/reg-event-fx
 ::add-bookmark-list
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (let [updated-db (update db :bookmarks conj (assoc bookmark :id (nano-id)))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx [[:dispatch [::close-modal]]]})))

(rf/reg-event-fx
 ::back-to-bookmark-list-modal
 (fn [_ [_ item]]
   {:fx [[:dispatch [::open-modal [bookmarks/add-to-bookmark-list-modal item]]]]}))

(rf/reg-event-fx
 ::add-bookmark-list-and-back
 (fn [_ [_ bookmark item]]
   {:fx [[:dispatch [::add-bookmark-list bookmark]]
         [:dispatch [::back-to-bookmark-list-modal item]]]}))

(rf/reg-event-fx
 ::remove-bookmark-list
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ id]]
   (let [updated-db (update db :bookmarks #(into [] (remove (fn [bookmark] (= (:id bookmark) id)) %)))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))})))

(rf/reg-event-fx
 ::add-to-likes
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (when-not (some #(= (:url %) (:url bookmark)) (-> db :bookmarks first :items))
     (let [updated-db (update-in db [:bookmarks 0 :items] #(into [] (conj (into [] %1) %2))
                                 (assoc bookmark :bookmark-id (-> db :bookmarks first :id)))]
       {:db    updated-db
        :store (assoc store :bookmarks (:bookmarks updated-db))}))))

(rf/reg-event-fx
 ::remove-from-likes
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (let [updated-db (update-in db [:bookmarks 0 :items] #(remove (fn [item] (= (:url item) (:url bookmark))) %))]
     {:db updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))})))

(rf/reg-event-fx
 ::add-to-bookmark-list
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark item]]
   (let [bookmark-list (first (filter #(= (:id %) (:id bookmark)) (:bookmarks db)))
         pos           (.indexOf (:bookmarks db) bookmark-list)
         updated-db    (if (some #(= (:url %) (:url item)) (:items bookmark-list))
                         db
                         (update-in db [:bookmarks pos :items] #(into [] (conj (into [] %1) %2))
                                    (assoc item :bookmark-id (:id bookmark))))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    [[:dispatch [::close-modal]]]})))

(rf/reg-event-fx
 ::remove-from-bookmark-list
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (let [bookmark-list (.indexOf (:bookmarks db) (first (filter #(= (:id %) (:bookmark-id bookmark)) (:bookmarks db))))
         updated-db (update-in db [:bookmarks bookmark-list :items] #(remove (fn [item] (= (:url item) (:url bookmark))) %))]
     {:db updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))})))

(rf/reg-event-db
 ::load-services
 (fn [db [_ res]]
   (assoc db :services (js->clj res :keywordize-keys true))))

(rf/reg-event-fx
 ::set-service-styles
 (fn [{:keys [db]} [_ res]]
   {:db db
    :fx [[:dispatch [::change-service-id (:service-id res)]]
         [:dispatch [::get-kiosks (:service-id res)]]]}))

(rf/reg-event-fx
 ::get-services
 (fn [{:keys [db]} _]
   (api/get-request "/services" [::load-services] [::bad-response])))

(rf/reg-event-db
 ::load-comments
 (fn [db [_ res]]
   (-> db
       (assoc-in [:stream :comments-page] (js->clj res :keywordize-keys true))
       (assoc-in [:stream :show-comments-loading] false))))

(rf/reg-event-fx
 ::get-comments
 (fn [{:keys [db]} [_ url]]
   (assoc
    (api/get-request (str "/comments/" (js/encodeURIComponent url))
                     [::load-comments] [::bad-response])
    :db (-> db
            (assoc-in [:stream :show-comments-loading] true)
            (assoc-in [:stream :show-comments] true)))))

(rf/reg-event-db
 ::toggle-stream-layout
 (fn [db [_ layout]]
   (assoc-in db [:stream layout] (not (-> db :stream layout)))))

(rf/reg-event-db
 ::toggle-comment-replies
 (fn [db [_ comment-id]]
   (update-in db [:stream :comments-page :comments]
              (fn [comments]
                (map #(if (= (:id %) comment-id)
                        (assoc % :show-replies (not (:show-replies %)))
                        %)
                     comments)))))

(rf/reg-event-db
 ::load-paginated-comments
 (fn [db [_ res]]
   (-> db
       (update-in [:stream :comments-page :comments] #(apply conj %1 %2)
                  (:comments (js->clj res :keywordize-keys true)))
       (assoc-in [:stream :comments-page :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::comments-pagination
 (fn [{:keys [db]} [_ url next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request (str "/comments/" (js/encodeURIComponent url))
                       [::load-paginated-comments] [::bad-response]
                       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-db
 ::load-kiosks
 (fn [db [_ res]]
   (assoc db :kiosks (js->clj res :keywordize-keys true))))

(rf/reg-event-fx
 ::get-kiosks
 (fn [{:keys [db]} [_ id]]
   (api/get-request (str "/services/" id "/kiosks")
                    [::load-kiosks] [::bad-response])))

(rf/reg-event-fx
 ::load-kiosk
 (fn [{:keys [db]} [_ res]]
   (let [kiosk-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :kiosk kiosk-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles kiosk-res]]
           [::document-title! (:id kiosk-res)]]})))

(rf/reg-event-fx
 ::get-default-kiosk-page
 (fn [{:keys [db]} [_ service-id]]
   (let [default-kiosk-id (when (= (js/parseInt service-id)
                                   (-> db :settings :default-service :service-id))
                            (-> db :settings :default-service :default-kiosk))]
     (if default-kiosk-id
       {:fx [[:dispatch [::get-kiosk-page service-id default-kiosk-id]]]}
       (assoc
        (api/get-request (str "/services/" service-id "/default-kiosk")
                         [::load-kiosk] [::bad-response])
        :db (assoc db :show-page-loading true))))))

(rf/reg-event-fx
 ::get-kiosk-page
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   (if kiosk-id
     (assoc
      (api/get-request (str "/services/" service-id "/kiosks/"
                            (js/encodeURIComponent kiosk-id))
                       [::load-kiosk] [::bad-response])
      :db (assoc db :show-page-loading true))
     {:fx [[:dispatch [::get-default-kiosk-page service-id]]]})))

(rf/reg-event-fx
 ::change-service-kiosk
 (fn [{:keys [db]} [_ service-id]]
   {:fx [[:dispatch [::change-service-id service-id]]
         [:dispatch
          [::navigate {:name :tubo.routes/kiosk
                       :params {}
                       :query  {:serviceId service-id}}]]]}))

(rf/reg-event-db
 ::load-paginated-kiosk-results
 (fn [db [_ res]]
   (-> db
       (update-in [:kiosk :related-streams] #(apply conj %1 %2)
                  (:related-streams (js->clj res :keywordize-keys true)))
       (assoc-in [:kiosk :next-page]
                 (:next-page (js->clj res :keywordize-keys true)))
       (assoc :show-pagination-loading false))))

(rf/reg-event-fx
 ::kiosk-pagination
 (fn [{:keys [db]} [_ service-id kiosk-id next-page-url]]
   (if (empty? next-page-url)
     {:db (assoc db :show-pagination-loading false)}
     (assoc
      (api/get-request
       (str "/services/" service-id "/kiosks/"
            (js/encodeURIComponent kiosk-id))
       [::load-paginated-kiosk-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-fx
 ::load-homepage
 (fn [{:keys [db]} [_ res]]
   (let [updated-db (assoc db :services (js->clj res :keywordize-keys true))
         service-id (:id (first
                          (filter #(= (-> db :settings :default-service :id)
                                      (-> % :info :name))
                                  (:services updated-db))))]
     {:fx [[:dispatch [::get-default-kiosk-page service-id]]
           [:dispatch [::change-service-id service-id]]]})))

(rf/reg-event-fx
 ::get-homepage
 (fn [{:keys [db]} _]
   (api/get-request "/services" [::load-homepage] [::bad-response])))

(rf/reg-event-fx
 ::load-audio-player-related-streams
 (fn [{:keys [db]} [_ res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:fx [[:dispatch [::enqueue-related-streams (:related-streams stream-res)]]]})))

(rf/reg-event-fx
 ::load-audio-player-stream
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ idx play? res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :show-audio-player-loading false)
      :fx (apply conj [[:dispatch [::change-media-queue-stream
                                   (-> stream-res :audio-streams first :content)
                                   idx]]]
                 (when play?
                   [[::player-src {:player      player
                                   :src         (-> stream-res :audio-streams first :content)
                                   :current-pos (:media-queue-pos db)}]
                    [::stream-metadata {:title   (:name stream-res)
                                        :artist  (:uploader-name stream-res)
                                        :artwork [{:src (:thumbnail-url stream-res)}]}]
                    [::media-session {:current-pos (:media-queue-pos db) :player player}]]))})))

(rf/reg-event-fx
 ::load-stream-page
 (fn [{:keys [db]} [_ res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :stream stream-res
                 :show-page-loading false)
      :fx [(when (and (-> db :settings :show-comments))
             [:dispatch [::get-comments (:url stream-res)]])
           [:dispatch [::set-service-styles stream-res]]
           [::document-title! (:name stream-res)]]})))

(rf/reg-event-fx
 ::fetch-stream-page
 (fn [{:keys [db]} [_ uri]]
   (api/get-request (str "/streams/" (js/encodeURIComponent uri))
                    [::load-stream-page] [::bad-response])))

(rf/reg-event-fx
 ::audio-player-stream-failure
 (fn [{:keys [db]} [_ play? res]]
   {:db (assoc db
               :show-audio-player-loading false
               :player-ready true)
    :fx [[:dispatch [::bad-response res]]
         (when play?
           (if (> (-> db :media-queue count) 1)
             [:dispatch [::change-media-queue-pos (-> db :media-queue-pos inc)]]
             [:dispatch [::dispose-audio-player]]))]}))

(rf/reg-event-fx
 ::fetch-audio-player-related-streams
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/streams/" (js/encodeURIComponent uri))
                     [::load-audio-player-related-streams] [::bad-response])
    :db (assoc db :show-audio-player-loading true))))

(rf/reg-event-fx
 ::fetch-audio-player-stream
 (fn [{:keys [db]} [_ stream idx play?]]
   (assoc
    (api/get-request (str "/streams/" (js/encodeURIComponent (:url stream)))
                     [::load-audio-player-stream idx play?]
                     [::audio-player-stream-failure play?])
    :db (assoc db :show-audio-player-loading true))))

(rf/reg-event-fx
 ::get-stream-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/streams/" (js/encodeURIComponent uri))
                     [::load-stream-page] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-channel
 (fn [{:keys [db]} [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :channel channel-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles channel-res]]
           [::document-title! (:name channel-res)]]})))

(rf/reg-event-fx
 ::get-channel-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request
     (str "/channels/" (js/encodeURIComponent uri))
     [::load-channel] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-playlist
 (fn [{:keys [db]} [_ res]]
   (let [playlist-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :playlist playlist-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles playlist-res]]
           [::document-title! (:name playlist-res)]]})))

(rf/reg-event-fx
 ::get-playlist-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/playlists/" (js/encodeURIComponent uri))
                     [::load-playlist] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-search-results
 (fn [{:keys [db]} [_ res]]
   (let [search-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :search-results search-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles search-res]]]})))

(rf/reg-event-fx
 ::get-search-page
 (fn [{:keys [db]} [_ service-id query]]
   (assoc
    (api/get-request (str "/services/" service-id "/search")
                     [::load-search-results] [::bad-response]
                     {:q query})
    :db (assoc db :show-page-loading true
               :show-search-form true)
    :fx [[::document-title! (str "Search for \"" query "\"")]])))

(rf/reg-event-fx
 ::change-setting
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ key val]]
   {:db    (assoc-in db [:settings key] val)
    :store (assoc store key val)}))

(rf/reg-event-fx
 ::load-settings-kiosks
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ service-name service-id res]]
   (let [kiosks-res    (js->clj res :keywordize-keys true)
         default-service-kiosk (-> db :settings :default-service :default-kiosk)
         default-kiosk (if (some #(= % default-service-kiosk) (:available-kiosks kiosks-res))
                         default-service-kiosk
                         (:default-kiosk kiosks-res))]
     {:db    (update-in db [:settings :default-service] assoc
                        :id service-name
                        :service-id service-id
                        :available-kiosks (:available-kiosks kiosks-res)
                        :default-kiosk default-kiosk)
      :store (update-in store [:default-service] assoc
                        :id service-name
                        :service-id service-id
                        :available-kiosks (:available-kiosks kiosks-res)
                        :default-kiosk default-kiosk)})))

(rf/reg-event-fx
 ::change-service-setting
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ val]]
   (let [service-id (-> (filter #(= val (-> % :info :name)) (:services db))
                        first
                        :id)]
     (api/get-request (str "/services/" service-id "/kiosks")
                      [::load-settings-kiosks val service-id] [::bad-response]))))

(rf/reg-event-fx
 ::change-kiosk-setting
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ val]]
   {:db    (assoc-in db [:settings :default-service :default-kiosk] val)
    :store (assoc-in store [:default-service :default-kiosk] val)}))

(rf/reg-event-fx
 ::get-settings-page
 (fn [{:keys [db]} _]
   (let [id (-> db :settings :default-service :id)
         service-id (-> db :settings :default-service :service-id)]
     (assoc
      (api/get-request (str "/services/" service-id "/kiosks")
                       [::load-settings-kiosks id service-id] [::bad-response])
      ::document-title! "Settings"))))

(rf/reg-event-fx
 ::get-bookmarks-page
 (fn [_]
   {::document-title! "Bookmarks"}))

(rf/reg-event-fx
 ::get-bookmark-page
 (fn [{:keys [db]} [_ playlist-id]]
   (let [playlist (first (filter #(= (:id %) playlist-id) (:bookmarks db)))]
     {::document-title! (:name playlist)})))
