(ns tubo.player.events
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [tubo.player.utils :as putils]
   [tubo.player.views :as views]
   [tubo.storage :refer [persist]]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-fx
 :player/volume
 (fn [{:keys [player volume]}]
   (when (and player @player)
     (set! (.-volume @player) (/ volume 100)))))

(rf/reg-fx
 :player/mute
 (fn [{:keys [player muted?]}]
   (when (and player @player)
     (set! (.-muted @player) muted?))))

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

(rf/reg-event-fx
 :player/initialize
 (fn [{:keys [db]} [_ stream player pos]]
   {:fx [[:player/configure [player (get-in db [:settings :video-codecs])]]
         [:player/request-filter [player (get-in db [:settings :instance])]]
         [:dispatch
          [:player/load
           player (putils/get-video-stream stream (:settings db)) pos]]]}))

(rf/reg-event-fx
 :player/start
 [(rf/inject-cofx ::inject/sub [:page-visible])]
 (fn [{:keys [page-visible]} [_ player stream]]
   (when page-visible
     {:fx [[:set-media-session-metadata
            {:title   (:name stream)
             :artist  (:uploader-name stream)
             :artwork [{:src (:thumbnail stream)}]}]
           [:set-media-session-handlers player]]})))

(defn load-video
  [player url element]
  (when (.-api @player)
    (-> (p/resolved nil)
        (p/then #(.attach (.-api @player) element))
        (p/then #(.load (.-api @player) url)))))

(rf/reg-event-fx
 :player/reload
 (fn [{:keys [db]} [_ player url]]
   {:promise
    {:call       #(load-video player
                              url
                              (.querySelector (.-shadowRoot @player)
                                              "video"))
     :on-success (when (get-in db [:settings :autoplay]) [:player/play player])
     :on-failure [:notifications/error "Playback failed"]}}))

(rf/reg-event-fx
 :player/on-load-failure
 (fn [{:keys [db]} [_ player error]]
   {:fx [[:dispatch
          [:notifications/error
           (if (seq (.-detail error))
             (.-detail error)
             "Playback failed. Retrying...")]]
         [:dispatch
          [:player/reload player
           (putils/get-video-stream (:stream db)
                                    (assoc (:settings db)
                                           :video-source-type
                                           "progressive-http"))]]]}))

(rf/reg-fx
 :player/set-next
 (fn [[player current-pos]]
   (when current-pos
     (set! (.-onended @player)
           #(rf/dispatch [:queue/change-pos (inc current-pos)])))))

(rf/reg-event-fx
 :player/load
 (fn [{:keys [db]} [_ player url pos]]
   {:promise         {:call       #(load-video player
                                               url
                                               (.querySelector (.-shadowRoot
                                                                @player)
                                                               "video"))
                      :on-success (when (get-in db [:settings :autoplay])
                                    [:player/play player])
                      :on-failure [:player/on-load-failure player]}
    :player/set-next [player pos]}))

(rf/reg-fx
 :player/loop
 (fn [{:keys [player loop]}]
   (set! (.-loop @player) loop)))

(rf/reg-fx
 :player/time
 (fn [{:keys [time player]}]
   (when @player
     (set! (.-currentTime @player) time))))

(rf/reg-event-fx
 :player/seek
 (fn [_ [_ time player]]
   {:player/time {:time time :player player}}))

(rf/reg-fx
 :player/pause
 (fn [{:keys [paused? player]}]
   (when (and player @player)
     (-> (if paused?
             (.play @player)
             (.pause @player))
         (p/catch #(rf/dispatch [:player/play-error % player]))))))

(rf/reg-event-fx
 :player/play
 (fn [_ [_ player]]
   {:player/pause {:paused? true :player player}}))

(rf/reg-event-fx
 :player/play-error
 (fn [_ [_ error player]]
   {:fx (case (.-code error)
          0 []
          1 []
          9 [[:dispatch [:notifications/error "Playback failed. Retrying..."]]
             [:dispatch [:queue/reload-current-stream player]]]
          [[:dispatch [:notifications/error (.-message error)]]])}))

(rf/reg-event-fx
 :player/on-error
 (fn [_ [_ player]]
   (when (and @player (.-error @player))
     {:fx (case (.. @player -error -code)
            4 [[:dispatch [:notifications/error "Playback failed. Retrying..."]]
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
 (fn [player]
   (when (gobj/containsKey js/navigator "mediaSession")
     (let [current-time (and player @player (.-currentTime @player))
           update-position
           #(.setPositionState js/navigator.mediaSession
                               {:duration     (.-duration @player)
                                :playbackRate (.-playbackRate @player)
                                :position     current-time})
           update-playback #(set! (.-playbackState js/navigator.mediaSession) %)
           seek #(do (rf/dispatch [:player/seek % player]) (update-position))
           ios? (or (re-find #"iPad|iPhone|iPod" js/navigator.userAgent)
                    (and (= js/navigator.platform "MacIntel")
                         (> js/navigator.maxTouchPoints 1)))
           events
           (cond-> {"play"          #(do (.play @player)
                                         (update-playback "playing"))
                    "pause"         #(do (.pause @player)
                                         (update-playback "paused"))
                    "previoustrack" #(rf/dispatch [:queue/previous])
                    "nexttrack"     #(rf/dispatch [:queue/next])
                    "seekto"        (fn [^js/navigator.MediaSessionActionDetails
                                         details]
                                      (seek (.-seekTime details)))
                    "stop"          #(seek 0)}
             (not ios?)
             (assoc "seekbackward"
                    (fn [^js/navigator.MediaSessionActionDetails details]
                      (seek (- (.-currentTime @player)
                               (or (.-seekOffset details) 10))))
                    "seekforward"
                    (fn [^js/navigator.MediaSessionActionDetails details]
                      (seek (+ (.-currentTime @player)
                               (or (.-seekOffset details) 10))))))]
       (doseq [[action cb] events]
         (try
           (.setActionHandler js/navigator.mediaSession action cb)
           (catch js/Error _
             (js/console.error (str "The media session action "
                                    action
                                    " is not supported.")))))))))

(rf/reg-event-fx
 :player/change-volume
 [persist]
 (fn [{:keys [db]} [_ value player]]
   {:db            (assoc db :player/volume value)
    :player/volume {:player player :volume value}}))

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
 :main-player/seek
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player]} [_ time]]
   {:player/time {:time time :player main-player}}))

(rf/reg-event-fx
 :main-player/play
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [db main-player]}]
   {:fx [(when (and (:bg-player/ready db) main-player @main-player)
           [:dispatch [:bg-player/pause true]])]}))

(rf/reg-event-fx
 :main-player/set-stream
 [(rf/inject-cofx ::inject/sub [:main-player])]
 (fn [{:keys [main-player db]} [_ stream pos]]
   (let [video-stream (putils/get-video-stream stream (:settings db))]
     {:fx [[:dispatch [:player/load main-player video-stream pos]]]})))

(rf/reg-event-fx
 :main-player/mount
 [(rf/inject-cofx ::inject/sub [:elapsed-time])
  (rf/inject-cofx ::inject/sub [:queue/current])]
 (fn [{:keys [db elapsed-time] :as cofx} [_ stream player pos]]
   {:db (assoc db :main-player/show true)
    :fx [[:player/configure [player (get-in db [:settings :video-codecs])]]
         [:player/request-filter [player (get-in db [:settings :instance])]]
         [:dispatch [:main-player/set-stream stream pos]]
         [:dispatch [:main-player/seek @elapsed-time]]
         [:dispatch
          [:player/change-volume (:player/volume db) player]]
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :comments-page]))
           [:dispatch
            [:comments/fetch-page (:url (:queue/current cofx))
             [:queue (:queue/position db)]]])
         (when-not (seq (get-in db
                                [:queue (:queue/position db)
                                 :related-items]))
           [:dispatch
            [:bg-player/fetch-stream (:url (:queue/current cofx))
             (:queue/position db) false]])]}))

(rf/reg-event-db
 :main-player/ready
 (fn [db [_ ready]]
   (assoc db :main-player/ready ready)))

(rf/reg-event-fx
 :main-player/show
 (fn []
   {:fx [[:dispatch [:queue/show false]]
         [:dispatch
          [:layout/show-mobile-panel
           {:id            (nano-id)
            :view          [views/main-player]
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
 [persist (rf/inject-cofx ::inject/sub [:elapsed-time])]
 (fn [{:keys [db elapsed-time]}]
   {:db (assoc db :main-player/show false)
    :fx [(when (and (> (count (:queue db)) 0) (not (:queue/show db)))
           [:dispatch [:bg-player/show]])
         (when (> (count (:queue db)) 0)
           [:dispatch [:bg-player/seek @elapsed-time]])
         (when (> (count (:queue db)) 0)
           [:dispatch [:bg-player/pause false]])]}))

(rf/reg-event-fx
 :bg-player/seek
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ time]]
   {:player/time {:time time :player bg-player}}))


(rf/reg-event-fx
 :bg-player/pause
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [bg-player]} [_ paused?]]
   {:player/pause {:paused? (not paused?)
                   :player  bg-player}}))

(rf/reg-event-db
 :bg-player/set-waiting
 (fn [db [_ val]]
   (assoc db :bg-player/waiting val)))

(rf/reg-event-fx
 :bg-player/set-stream
 [(rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [db bg-player]} [_ stream pos]]
   (when-let [audio-stream (putils/get-audio-stream stream (:settings db))]
     {:fx [[:dispatch [:player/load bg-player audio-stream pos]]]
      :db (assoc db :bg-player/loading false)})))

(rf/reg-event-fx
 :bg-player/mount
 (fn [{:keys [db]} [_ stream player pos]]
   {:fx [[:player/configure [player (get-in db [:settings :video-codecs])]]
         [:player/request-filter [player (get-in db [:settings :instance])]]
         [:dispatch [:bg-player/set-stream stream pos]]
         [:dispatch
          [:player/change-volume (:player/volume db) player]]]}))

(rf/reg-event-fx
 :bg-player/unmount
 (fn []
   {:fx [[:dispatch [:bg-player/set-ready false]]
         [:clear-media-session]]}))

(rf/reg-event-fx
 :bg-player/mute
 [persist (rf/inject-cofx ::inject/sub [:bg-player])]
 (fn [{:keys [db bg-player]} [_ value]]
   {:db          (assoc db :player/muted value)
    :player/mute {:player bg-player :muted? value}}))

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
    :fx [[:dispatch [:bg-player/pause true]]
         [:dispatch [:bg-player/seek 0]]
         [:dispatch [:queue/show false]]
         [:timeout
          {:id    (nano-id)
           :event [:bg-player/hide]
           :time  200}]]}))

(rf/reg-event-db
 :bg-player/set-ready
 (fn [db [_ ready]]
   (assoc db :bg-player/ready ready)))

(rf/reg-event-fx
 :bg-player/load-related-items
 (fn [_ [_ notify? {:keys [body]}]]
   {:fx [[:dispatch [:queue/add-n (:related-items body) notify?]]]}))

(rf/reg-event-fx
 :bg-player/fetch-related-items
 (fn [{:keys [db]} [_ url]]
   {:fx [[:dispatch
          [:stream/fetch url
           [:bg-player/load-related-items false]] [:bad-response]]]
    :db (assoc db :bg-player/loading true)}))

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
               :bg-player/show    (not (:main-player/show db))
               :bg-player/loading false)
    :fx [[:dispatch [:queue/change-stream body idx play?]]]}))

(rf/reg-event-fx
 :bg-player/bad-response
 (fn [{:keys [db]} [_ idx play? res]]
   {:db (assoc db
               :bg-player/loading
               false)
    :fx [[:dispatch [:bad-response res]]
         (when play?
           (if (> (-> db
                      :queue
                      count)
                  1)
             [:dispatch [:queue/change-pos (inc idx)]]
             [:dispatch [:bg-player/dispose]]))]}))

(rf/reg-event-fx
 :bg-player/fetch-stream
 (fn [{:keys [db]} [_ url idx play?]]
   (merge (when-not (nil? play?)
            {:db (assoc db :bg-player/loading play?)})
          {:fx [(when play?
                  [:dispatch [:start-loading]])
                [:dispatch
                 [:api/get (str "streams/" (js/encodeURIComponent url))
                  (if play?
                    [:on-success [:bg-player/load-stream idx play?]]
                    [:bg-player/load-stream idx play?])
                  (if-not (nil? play?)
                    [:bg-player/bad-response idx play?]
                    [:noop])]]]})))
