(ns tubo.events
  (:require
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [day8.re-frame.http-fx]
   [goog.object :as gobj]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [tubo.api :as api]
   [vimsical.re-frame.cofx.inject :as inject]))

(reg-co-fx! :tubo {:fx :store :cofx :store})

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [store]} _]
   (let [{:keys [current-theme show-comments show-related show-description
                 media-queue media-queue-pos show-audio-player
                 loop-playback volume-level muted bookmarks]} store]
     {:db
      {:search-query      ""
       :service-id        0
       :stream            {}
       :search-results    []
       :services          []
       :loop-playback     (if (nil? loop-playback) :playlist loop-playback)
       :media-queue       (if (nil? media-queue) [] media-queue)
       :media-queue-pos   (if (nil? media-queue-pos) 0 media-queue-pos)
       :volume-level      (if (nil? volume-level) 100 volume-level)
       :bookmarks         (if (nil? bookmarks) [] bookmarks)
       :muted             (if (nil? muted) false muted)
       :paused            true
       :current-match     nil
       :show-audio-player (if (nil? show-audio-player) false show-audio-player)
       :settings
       {:current-theme    (if (nil? current-theme) :light current-theme)
        :themes           #{:light :dark}
        :show-comments    (if (nil? show-comments) true show-comments)
        :show-related     (if (nil? show-related) true show-related)
        :show-description (if (nil? show-description) true show-description)}}})))

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

(rf/reg-event-fx
 ::player-paused
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ paused?]]
   {:db            (assoc db :paused paused?)
    ::player-pause {:paused? (not paused?)
                    :player  player}}))

(rf/reg-event-fx
 ::player-start
 [(rf/inject-cofx ::inject/sub [:player]) (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db player]} _]
   {:fx [[:dispatch [::player-paused false]]
         [::player-volume {:player player :volume (:volume-level db)}]]}))

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
 ::toggle-search-form
 (fn [db _]
   (assoc db :show-search-form (not (:show-search-form db)))))

(rf/reg-event-fx
 ::toggle-mobile-nav
 (fn [{:keys [db]} _]
   {:db (assoc db :show-mobile-nav (not (:show-mobile-nav db)))
    ::body-overflow! (not (:show-mobile-nav db))}))

(rf/reg-event-fx
 ::toggle-media-queue
 (fn [{:keys [db]} _]
   {:db (assoc db :show-media-queue (not (:show-media-queue db)))
    ::body-overflow! (not (:show-media-queue db))}))

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
              (assoc :show-media-queue false)
              (assoc :show-mobile-nav false)
              (assoc :show-pagination-loading false))
      ::scroll-to-top nil
      ::body-overflow! false})))

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

(rf/reg-event-db
 ::change-service-id
 (fn [db [_ service-id]]
   (assoc db :service-id service-id)))

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
       (str "/api/channels/" (js/encodeURIComponent uri) )
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
       (str "/api/playlists/" (js/encodeURIComponent uri))
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
       (str "/api/services/" id "/search")
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
 ::change-media-queue-pos
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ i]]
   (let [idx    (if (< i (count (:media-queue db)))
                  i
                  (when (= (:loop-playback db) :playlist) 0))
         stream (get (:media-queue db) idx)]
     (when stream
       {:db    (-> db
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
               (assoc :media-queue [])
               (assoc :media-queue-pos 0)))]
     {:db    (remove-entries db)
      :store (remove-entries store)
      :fx [[:dispatch [::player-paused true]]
           [:dispatch [::set-player-time 0]]]})))

(rf/reg-event-fx
 ::switch-to-audio-player
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ stream service-color]]
   (let [full-stream (conj {:service-color service-color} stream)
         updated-db (update db :media-queue conj full-stream)
         idx        (.indexOf (:media-queue updated-db) full-stream)]
     {:db    (-> updated-db
                 (assoc :show-audio-player true))
      :store (-> store
                 (assoc :show-audio-player true)
                 (assoc :media-queue (:media-queue updated-db)))
      :fx    [[:dispatch [::fetch-audio-player-stream (:url stream) idx (= (count (:media-queue db)) 0)]]]})))

(rf/reg-event-fx
 ::enqueue-related-streams
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ streams service-color]]
   {:db (assoc db :show-audio-player true)
    :store (assoc store :show-audio-player true)
    :fx (into [] (conj
                  (map #(identity [:dispatch
                                   [::add-to-media-queue
                                    (conj {:service-color service-color} %)]])
                       streams)
                  [:dispatch [::fetch-audio-player-stream (:url (first streams))
                              (count (:media-queue db)) (= (count (:media-queue db)) 0)]]))}))

(rf/reg-event-fx
 ::add-to-bookmarks
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (when-not (some #(= (:url %) (:url bookmark)) (:bookmarks db))
     (let [updated-db (update db :bookmarks conj bookmark)]
       {:db    updated-db
        :store (assoc store :bookmarks (:bookmarks updated-db))}))))

(rf/reg-event-fx
 ::remove-from-bookmarks
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (let [updated-db (update db :bookmarks #(remove (fn [item] (= (:url item) (:url bookmark))) %))]
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
   (api/get-request "/api/services" [::load-services] [::bad-response])))

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
    (api/get-request (str "/api/comments/" (js/encodeURIComponent url))
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
      (api/get-request (str "/api/comments/" (js/encodeURIComponent url))
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
   (api/get-request (str "/api/services/" id "/kiosks")
                    [::load-kiosks] [::bad-response])))

(rf/reg-event-fx
 ::load-kiosk
 (fn [{:keys [db]} [_ res]]
   (let [kiosk-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :kiosk kiosk-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles kiosk-res]]]})))

(rf/reg-event-fx
 ::get-default-kiosk-page
 (fn [{:keys [db]} [_ service-id]]
   (assoc
    (api/get-request (str "/api/services/" service-id "/default-kiosk")
                     [::load-kiosk] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::get-kiosk-page
 (fn [{:keys [db]} [_ service-id kiosk-id]]
   (if kiosk-id
     (assoc
      (api/get-request (str "/api/services/" service-id "/kiosks/"
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
       (str "/api/services/" service-id "/kiosks/"
            (js/encodeURIComponent kiosk-id))
       [::load-paginated-kiosk-results] [::bad-response]
       {:nextPage (js/encodeURIComponent next-page-url)})
      :db (assoc db :show-pagination-loading true)))))

(rf/reg-event-fx
 ::load-audio-player-stream
 [(rf/inject-cofx ::inject/sub [:player])]
 (fn [{:keys [db player]} [_ idx play? res]]
   (let [stream-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :show-audio-player-loading false
                 :paused false)
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
      :fx [[:dispatch [::change-stream-format nil]]
           (when (and (-> db :settings :show-comments))
             [:dispatch [::get-comments (:url stream-res)]])
           [:dispatch [::set-service-styles stream-res]]]})))

(rf/reg-event-fx
 ::fetch-stream-page
 (fn [{:keys [db]} [_ uri]]
    (api/get-request (str "/api/streams/" (js/encodeURIComponent uri))
                     [::load-stream-page] [::bad-response])))

(rf/reg-event-fx
 ::fetch-audio-player-stream
 (fn [{:keys [db]} [_ uri idx play?]]
   (assoc
    (api/get-request (str "/api/streams/" (js/encodeURIComponent uri))
                     [::load-audio-player-stream idx play?] [::bad-response])
    :db (assoc db :show-audio-player-loading true))))

(rf/reg-event-fx
 ::get-stream-page
 (fn [{:keys [db]} [_ uri]]
   {:db (assoc db :show-page-loading true)
    :fx [[:dispatch [::fetch-stream-page uri]]]}))

(rf/reg-event-db
 ::change-stream-format
 (fn [{:keys [stream] :as db} [_ format-id]]
   (let [{:keys [audio-streams video-streams]} stream]
     (if format-id
       (assoc db :stream-format
              (first (filter #(= format-id (:id %))
                             (apply conj audio-streams video-streams))))
       (assoc db :stream-format (if (empty? video-streams)
                                  (first audio-streams)
                                  (last video-streams)))))))

(rf/reg-event-fx
 ::load-channel
 (fn [{:keys [db]} [_ res]]
   (let [channel-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :channel channel-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles channel-res]]]})))

(rf/reg-event-fx
 ::get-channel-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request
     (str "/api/channels/" (js/encodeURIComponent uri))
     [::load-channel] [::bad-response])
    :db (assoc db :show-page-loading true))))

(rf/reg-event-fx
 ::load-playlist
 (fn [{:keys [db]} [_ res]]
   (let [playlist-res (js->clj res :keywordize-keys true)]
     {:db (assoc db :playlist playlist-res
                 :show-page-loading false)
      :fx [[:dispatch [::set-service-styles playlist-res]]]})))

(rf/reg-event-fx
 ::get-playlist-page
 (fn [{:keys [db]} [_ uri]]
   (assoc
    (api/get-request (str "/api/playlists/" (js/encodeURIComponent uri))
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
    (api/get-request (str "/api/services/" service-id "/search")
                     [::load-search-results] [::bad-response]
                     {:q query})
    :db (assoc db :show-page-loading true
               :show-search-form true))))

(rf/reg-event-fx
 ::change-setting
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ key val]]
   {:db    (assoc-in db [:settings key] val)
    :store (assoc store key val)}))
