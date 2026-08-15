(ns tubo.player.events
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [tubo.interceptors :refer [persist]]
   [tubo.player.views :as views]
   [tubo.utils :as utils]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-fx
 :player/volume
 (fn [[player volume]]
   (when (and player @player)
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 :player/mute
 (fn [[player muted?]]
   (when (and player @player)
     (set! (.-muted @player) muted?))))

(rf/reg-event-fx
 :player/mute
 (fn [{:keys [db]} [_ player value]]
   {:db          (assoc db :player/muted value)
    :player/mute [player value]}))

(rf/reg-fx
 :player/configure
 (fn [[player video-codecs]]
   (let [codecs                 [{:name "av1"
                                  :shaka-codec "av01"
                                  :type
                                  "video/mp4; codecs=\"av01.0.08M.08\""}
                                 {:name        "vp9"
                                  :shaka-codec "vp9"
                                  :type        "video/webm; codecs=\"vp9\""}
                                 {:name "avc"
                                  :shaka-codec "avc1"
                                  :type
                                  "video/mp4; codecs=\"avc1.4d401f\""}]
         preferred-video-codecs (->> (filter
                                      #(and
                                        (seq (.canPlayType @player (:type %)))
                                        (str/includes? video-codecs (:name %)))
                                      codecs)
                                     (map :shaka-codec))]
     (.configure (.-api @player)
                 (clj->js
                  {"preferredVideoCodecs" preferred-video-codecs
                   "preferredAudioCodecs" ["opus" "mp4a"]
                   "manifest"             {"disableVideo" false}
                   "streaming"            {"retryParameters"
                                           {"maxAttempts"   js/Infinity
                                            "baseDelay"     250
                                            "backoffFactor" 1.5}}})))))

(rf/reg-fx
 :player/request-filter
 (fn [[player url]]
   (when-let [networking-engine (.getNetworkingEngine (.-api @player))]
     (.registerRequestFilter
      networking-engine
      (fn [_ request]
        (let [original-url (some->> (.-uris request)
                                    first
                                    (new js/URL))
              proxied-url  (when (or (str/ends-with? (.-host original-url)
                                                     ".googlevideo.com")
                                     (str/ends-with? (.-host original-url)
                                                     ".bcbits.com"))
                             (new js/URL original-url))]
          (set! (.-retryParameters request)
                (clj->js {"maxAttempts"   js/Infinity
                          "baseDelay"     250
                          "backoffFactor" 1.5}))
          (when proxied-url
            (when (seq (.-Range (.-headers request)))
              (.. proxied-url
                  -searchParams
                  (set "range"
                       (get (str/split (.-Range (.-headers request)) #"=") 1)))
              (set! (.-headers request) #js {}))
            (aset (.-uris request)
                  0
                  (str url
                       "/proxy/"
                       (js/encodeURIComponent
                        (.toString proxied-url)))))))))))

(rf/reg-fx
 :player/register
 (fn [[!players id !player]]
   (swap! !players assoc id !player)))

(rf/reg-fx
 :player/unregister
 (fn [[!players id]]
   (swap! !players dissoc id)))

(rf/reg-event-fx
 :player/register
 [(rf/inject-cofx :players)]
 (fn [{:keys [players]} [_ id !player]]
   {:player/register [players id !player]}))

(rf/reg-event-fx
 :player/unregister
 [(rf/inject-cofx :players)]
 (fn [{:keys [players]} [_ id]]
   {:player/unregister [players id]}))

(rf/reg-event-fx
 :player/start
 [(rf/inject-cofx ::inject/sub [:page-visible])]
 (fn [{:keys [page-visible]} [_ !player stream queue?]]
   (when page-visible
     {:fx [[:set-media-session-metadata
            {:title   (:name stream)
             :artist  (:uploader-name stream)
             :artwork [{:src (:thumbnail stream)}]}]
           [:set-media-session-handlers [!player queue?]]]})))

(defn- set-quality!
  [player default-resolution]
  (when (and @player (not= default-resolution "Best"))
    (let [target-height (js/parseInt default-resolution)
          renditions    (.-videoRenditions @player)
          cnt           (.-length renditions)]
      (when-let [best-idx (->> (range cnt)
                               (map #(vector % (.-height (aget renditions %))))
                               (filter #(<= (second %) target-height))
                               (sort-by second >)
                               ffirst)]
        (set! (.-selectedIndex renditions) best-idx)))))

(defn load-video
  [player url default-resolution]
  (when @player
    (js/Promise.
     (fn [resolve reject]
       (letfn [(on-error [err]
                 (.removeEventListener @player "loadeddata" on-loaded)
                 (reject err))
               (on-loaded [evt]
                 (.removeEventListener @player "error" on-error)
                 (set-quality! player default-resolution)
                 (resolve evt))]
         (.addEventListener @player "error" on-error #js {:once true})
         (.addEventListener @player "loadeddata" on-loaded #js {:once true})
         (set! (.-src @player) url))))))

(rf/reg-fx
 :player/set-next
 (fn [[player current-pos]]
   (when current-pos
     (set! (.-onended @player)
           #(rf/dispatch [:queue/change-pos (inc current-pos)])))))

(rf/reg-event-fx
 :player/on-load-failure
 (fn [{:keys [db]} [_ player stream pos error]]
   {:fx [[:dispatch
          [:notifications/error
           (if (seq (.-detail error))
             (.-detail error)
             "Playback failed. Retrying...")]]
         [:dispatch
          [:player/load player
           stream
           pos
           (utils/get-stream-url stream
                                 (assoc (:settings db)
                                        :stream-protocol
                                        "progressive-http"))
           [:notifications/error "Playback failed"]]]]}))

(rf/reg-event-fx
 :player/load
 (fn [{:keys [db]} [_ player stream pos fallback-url on-failure]]
   (when-let [url (or fallback-url
                      (utils/get-stream-url stream (:settings db)))]
     (cond-> {:promise {:call       #(load-video
                                      player
                                      url
                                      (get-in db
                                              [:settings :default-resolution]))
                        :on-success (when (get-in db [:settings :autoplay])
                                      [:player/pause player false])
                        :on-failure (or on-failure
                                        [:player/on-load-failure player stream
                                         pos])}}
       pos (assoc :player/set-next [player pos])))))

(rf/reg-fx
 :player/loop
 (fn [[player loop]]
   (set! (.-loop @player) loop)))

(rf/reg-fx
 :player/time
 (fn [[player time]]
   (when @player
     (set! (.-currentTime @player) time))))

(rf/reg-event-fx
 :player/seek
 (fn [_ [_ player time]]
   {:player/time [player time]}))

(rf/reg-fx
 :player/pause
 (fn [[player value]]
   (when (and player @player)
     (-> (if value
             (.pause @player)
             (.play @player))
         (p/catch #(rf/dispatch [:player/playback-error player %]))))))

(rf/reg-event-fx
 :player/pause
 (fn [_ [_ player value]]
   {:player/pause [player value]}))

(rf/reg-event-fx
 :player/play
 (fn [_ [_ !player stream queue?]]
   {:fx [[:dispatch [:player/start !player stream queue?]]
         [:dispatch [:player/set-playback-state "playing"]]]}))

(defn- compute-buffered
  [el]
  (let [len (.. el -buffered -length)]
    (when (and (.-duration el) (> len 0))
      (if (= (.end (.-buffered el) (- len 1)) (.-duration el))
        100
        (when (< (.start (.-buffered el) (- len 1)) (.-currentTime el))
          (* (/ (.end (.-buffered el) (- len 1)) (.-duration el)) 100))))))

(rf/reg-fx
 :player/progress
 (fn [[!player !buffered]]
   (when-let [el (and !player @!player)]
     (when-let [v (compute-buffered el)]
       (reset! !buffered v)))))

(rf/reg-event-fx
 :player/progress
 [(rf/inject-cofx :buffered)]
 (fn [{:keys [buffered]} [_ !player]]
   {:player/progress [!player buffered]}))

(rf/reg-fx
 :player/update
 (fn [[!player !elapsed !buffered !waiting]]
   (when @!waiting (reset! !waiting false))
   (when-let [el (and !player @!player)]
     (when-let [v (compute-buffered el)]
       (reset! !buffered v))
     (reset! !elapsed (.-currentTime el)))))

(rf/reg-event-fx
 :player/update
 [(rf/inject-cofx :elapsed) (rf/inject-cofx :buffered)
  (rf/inject-cofx :waiting)]
 (fn [{:keys [elapsed buffered waiting]} [_ !player]]
   {:player/update [!player elapsed buffered waiting]}))

(rf/reg-event-fx
 :player/playback-error
 (fn [_ [_ player error]]
   {:fx (case (.-code error)
          0 []
          1 []
          9 [[:dispatch [:notifications/error "Playback failed. Retrying..."]]
             [:dispatch [:queue/reload-current-stream player]]]
          [[:dispatch [:notifications/error (.-message error)]]])}))

(rf/reg-event-fx
 :player/media-error
 (fn [_ [_ player]]
   (when (and player @player (.-error @player))
     {:fx (case (.. @player -error -code)
            4 [[:dispatch
                [:notifications/error "Playback failed. Retrying..."]]
               [:dispatch [:queue/reload-current-stream player]]]
            [[:dispatch
              [:notifications/error (.. @player -error -message)]]])})))

(rf/reg-fx
 :set-media-session-metadata
 (fn [metadata]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-metadata js/navigator.mediaSession)
           (js/MediaMetadata. (clj->js metadata))))))

(rf/reg-fx
 :clear-media-session
 (fn [_]
   (.setPositionState js/navigator.mediaSession)))

(rf/reg-fx
 :set-media-session-playback-state
 (fn [state]
   (when (gobj/containsKey js/navigator "mediaSession")
     (set! (.-playbackState js/navigator.mediaSession) state))))

(rf/reg-event-fx
 :player/set-playback-state
 (fn [_ [_ state]]
   {:set-media-session-playback-state state}))

(rf/reg-fx
 :set-media-session-handlers
 (fn [[player queue?]]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [current-time (and player @player (.-currentTime @player))
           update-position
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     current-time})
           update-playback #(set! (.-playbackState js/navigator.mediaSession) %)
           seek #(do (rf/dispatch [:player/seek player %]) (update-position))
           ios? (or (re-find #"iPad|iPhone|iPod" js/navigator.userAgent)
                    (and (= js/navigator.platform "MacIntel")
                         (> js/navigator.maxTouchPoints 1)))
           events {"play" #(do (.play @player)
                               (update-playback "playing"))
                   "pause" #(do (.pause @player)
                                (update-playback "paused"))
                   "seekto" (fn [^js/navigator.MediaSessionActionDetails
                                 details]
                              (seek (.-seekTime details)))
                   "stop" #(seek 0)
                   "previoustrack" (when queue?
                                     #(rf/dispatch [:queue/previous player]))
                   "nexttrack" (when queue? #(rf/dispatch [:queue/next]))
                   "seekbackward"
                   (when (or (not queue?) (not ios?))
                     (fn [^js/navigator.MediaSessionActionDetails details]
                       (seek (- (.-currentTime @player)
                                (or (.-seekOffset details) 10)))))
                   "seekforward"
                   (when (or (not queue?) (not ios?))
                     (fn [^js/navigator.MediaSessionActionDetails details]
                       (seek (+ (.-currentTime @player)
                                (or (.-seekOffset details) 10)))))}]
       (doseq [[action cb] events]
         (try
           (.setActionHandler js/navigator.mediaSession action cb)
           (catch js/Error _
             (js/console.error (str "The media session action "
                                    action
                                    " is not supported.")))))))))

(rf/reg-event-fx
 :player/volume
 [persist]
 (fn [{:keys [db]} [_ player value]]
   {:db            (assoc db :player/volume value)
    :player/volume [player value]}))

(rf/reg-event-fx
 :player/loop
 [persist]
 (fn [{:keys [db]} _]
   (let [loop-state (case (:player/loop db)
                      :stream   false
                      :playlist :stream
                      :playlist)]
     {:db (assoc db :player/loop loop-state)})))

(rf/reg-event-fx
 :main-player/mount
 (fn [{:keys [db]} [_ stream]]
   {:db (assoc db :main-player/show true)
    :fx [(when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :comments-page]))
           [:dispatch
            [:comments/fetch-page (:url stream)
             [:queue (:queue/position db)]]])
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :related-items]))
           [:dispatch
            [:bg-player/fetch-stream (:url stream) (:queue/position db)
             false]])]}))

(rf/reg-event-fx
 :main-player/show
 (fn [{:keys [db]}]
   {:fx [[:dispatch
          [:layout/show-mobile-panel
           {:id            (nano-id)
            :view          [views/main-player (:bg-player/id db)]
            :extra-classes ["h-[calc(100dvh-56px)]" "bg-neutral-100"
                            "dark:bg-neutral-950"]}]]]}))

(rf/reg-event-db
 :main-player/toggle-layout
 (fn [db [_ layout]]
   (assoc-in db
    [:queue (:queue/position db) layout]
    (not (get-in db [:queue (:queue/position db) layout])))))

(rf/reg-event-fx
 :main-player/unmount
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :main-player/show false)
    :fx [(when (and (> (count (:queue db)) 0) (not (:queue/show db)))
           [:dispatch [:bg-player/show]])]}))

(rf/reg-event-fx
 :bg-player/load
 [(rf/inject-cofx :players)]
 (fn [{:keys [db players]} [_ stream pos]]
   (when-let [!player (get @players (:bg-player/id db))]
     {:fx [[:dispatch [:player/load !player stream pos]]]})))

(rf/reg-event-fx
 :bg-player/mount
 (fn [{:keys [db]} [_ id !player stream pos]]
   {:db (assoc db :bg-player/id id)
    :fx [[:dispatch [:player/register id !player]]
         [:player/configure [!player (get-in db [:settings :video-codecs])]]
         [:player/request-filter [!player (get-in db [:settings :instance])]]
         [:dispatch [:bg-player/load stream pos]]
         [:dispatch [:player/volume !player (:player/volume db)]]]}))

(rf/reg-event-fx
 :bg-player/unmount
 (fn [{:keys [db]} [_ id]]
   {:db (dissoc db :bg-player/id)
    :fx [[:dispatch [:player/unregister id]]
         [:clear-media-session]]}))

(rf/reg-event-fx
 :bg-player/hide
 [persist]
 (fn [{:keys [db]}]
   {:db (assoc db :bg-player/show false)}))

(rf/reg-event-fx
 :bg-player/dispose
 [persist]
 (fn [{:keys [db]}]
   {:db (-> db
            (assoc :queue [])
            (assoc :queue/unshuffled nil)
            (assoc :queue/position 0)
            (assoc :player/shuffled false))
    :fx [[:dispatch [:queue/show false]]
         [:timeout
          {:id    (nano-id)
           :event [:bg-player/hide]
           :time  200}]]}))


(rf/reg-event-fx
 :bg-player/load-related-items
 (fn [_ [_ notify? {:keys [body]}]]
   {:fx [[:dispatch [:queue/add-n (:related-items body) notify?]]]}))

(rf/reg-event-fx
 :bg-player/fetch-related-items
 (fn [_ [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:bg-player/load-related-items false]] [:bad-response]]]}))

(rf/reg-event-fx
 :bg-player/start-radio
 (fn [{:keys [db]} [_ stream]]
   (let [updated-db (update db :queue conj stream)
         idx        (.lastIndexOf (:queue updated-db) stream)]
     {:fx [[:dispatch [:queue/add stream]]
           [:dispatch [:bg-player/fetch-stream (:url stream) idx true]]
           [:dispatch [:bg-player/fetch-related-items (:url stream)]]
           [:dispatch
            [:notifications/add
             {:status-text "Started stream radio"
              :type        :info}]]]})))

(rf/reg-event-fx
 :bg-player/show
 (fn [{:keys [db]}]
   {:db (assoc db :bg-player/show true)}))

(rf/reg-event-fx
 :bg-player/load-stream
 [persist]
 (fn [{:keys [db]} [_ idx play? {:keys [body]}]]
   {:db (assoc db
               :bg-player/show
               (not (:main-player/show db)))
    :fx [[:dispatch [:queue/change-stream body idx play?]]]}))

(rf/reg-event-fx
 :bg-player/bad-response
 (fn [{:keys [db]} [_ idx play? res]]
   {:fx [[:dispatch [:bad-response res]]
         (when play?
           (if (> (-> db
                      :queue
                      count)
                  1)
             [:dispatch [:queue/change-pos (inc idx)]]
             [:dispatch [:bg-player/dispose]]))]}))

(rf/reg-event-fx
 :bg-player/fetch-stream
 (fn [_ [_ url idx play?]]
   {:fx [(when play?
           [:dispatch [:start-loading]])
         [:dispatch
          [:api/get (str "streams/" (js/encodeURIComponent url))
           (if play?
             [:on-success [:bg-player/load-stream idx play?]]
             [:bg-player/load-stream idx play?])
           (if-not (nil? play?)
             [:bg-player/bad-response idx play?]
             [:noop])]]]}))

(rf/reg-event-fx
 :stream-player/load
 [(rf/inject-cofx :players)]
 (fn [{:keys [db players]} [_ stream]]
   (when-let [!player (get @players (:stream-player/id db))]
     {:fx [[:dispatch [:player/load !player stream]]]})))

(rf/reg-event-fx
 :stream-player/mount
 (fn [{:keys [db]} [_ stream id !player]]
   {:db (assoc db :stream-player/id id)
    :fx (cond-> [[:dispatch [:player/register id !player]]
                 [:player/configure
                  [!player (get-in db [:settings :video-codecs])]]
                 [:player/request-filter
                  [!player (get-in db [:settings :instance])]]]
          stream (conj [:dispatch [:stream-player/load stream]]))}))

(rf/reg-event-fx
 :stream-player/unmount
 (fn [{:keys [db]} [_ id]]
   {:db (dissoc db :stream-player/id)
    :fx [[:dispatch [:player/unregister id]]
         [:clear-media-session]]}))
