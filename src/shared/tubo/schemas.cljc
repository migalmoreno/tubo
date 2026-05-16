(ns tubo.schemas
  (:require
   [malli.core :as m]
   [malli.transform :as transform]))

(def ValidUsername
  [:and
   [:string {:min 3 :max 22}]
   [:re
    {:error/message
     "should be alphanumeric or contain the characters '.', '_', '-'"}
    #"^[a-zA-Z0-9._-]+$"]])

(def ValidPassword
  [:and
   [:string {:min 8 :max 32}]
   [:re {:error/message "should contain one number"} #"[0-9]+"]
   [:re {:error/message "should contain one upper case letter"} #"[A-Z]+"]
   [:re {:error/message "should contain one lower case letter"} #"[a-z]+"]
   [:re {:error/message "should contain one special character"} #"[^\w\s]+"]])

(def UserPlaylistStream
  [:map
   [:id {:optional true} [:maybe int?]]
   [:name string?]
   [:duration [:maybe int?]]
   [:uploader-avatar [:maybe uri?]]
   [:thumbnail [:maybe uri?]]
   [:uploader-url uri?]
   [:uploader-verified boolean?]
   [:uploader-name string?]
   [:url uri?]])

(def UserPlaylist
  [:or
   [:map
    [:id {:optional true} int?]
    [:playlist-id {:optional true} uuid?]
    [:name string?]
    [:thumbnail [:maybe uri?]]
    [:owner {:optional true} int?]
    [:items {:optional true} [:vector UserPlaylistStream]]]
   [:map
    [:items [:vector UserPlaylistStream]]]])

(def PeerTubeInstance
  [:map
   [:name string?]
   [:url string?]])

(def PlaylistsConfigFile
  [:map
   [:format
    [:fn {:error/fn "Format is not supported"} (fn [format] (= format "Tubo"))]]
   [:version int?]
   [:playlists
    [:vector
     [:map
      [:name string?]
      [:thumbnail {:optional true} [:maybe uri?]]
      [:items [:vector string?]]]]]])

(def SubscriptionChannel
  [:map
   [:name string?]
   [:url string?]
   [:avatar string?]
   [:verified boolean?]])

(def Theme [:enum "auto" "light" "dark"])

(def ItemLayout [:enum "list" "grid"])

(def ImageQuality [:enum "high" "medium" "low" "none"])

(def Loop [:enum :playlist :stream false])

(def SourceType [:enum "dash" "hls" "progressive-http"])

(def Settings
  [:map {:closed true}
   [:theme {:optional true} Theme]
   [:show-comments {:optional true} boolean?]
   [:show-related {:optional true} boolean?]
   [:show-description {:optional true} boolean?]
   [:items-layout {:optional true} ItemLayout]
   [:autoplay {:optional true} boolean?]
   [:video-codecs {:optional true} string?]
   [:video-source-type {:optional true} SourceType]
   [:audio-source-type {:optional true} SourceType]
   [:default-resolution {:optional true} string?]
   [:default-video-format {:optional true} string?]
   [:default-audio-format {:optional true} string?]
   [:seamless-playback {:optional true} boolean?]
   [:instance {:default "http://localhost:3000"} string?]
   [:auth-instance {:default "http://localhost:3000"} string?]
   [:image-quality {:optional true} ImageQuality]
   [:default-country {:optional true} map?]
   [:default-kiosk {:optional true} map?]
   [:default-filter {:optional true} map?]
   [:default-service {:optional true} [:maybe int?]]])

(def LocalDB
  [:map {:closed true}
   [:db-loaded? {:optional true} [:maybe boolean?]]
   [:navigation/current-match {:optional true} [:maybe map?]]
   [:navigation/show-mobile-menu {:optional true} [:maybe boolean?]]
   [:navigation/show-title {:optional true} [:maybe boolean?]]
   [:navigation/show-sidebar {:optional true} [:maybe boolean?]]
   [:player/muted {:optional true :persist true} [:maybe boolean?]]
   [:player/shuffled {:optional true :persist true} [:maybe boolean?]]
   [:player/loop {:default :playlist :persist true} Loop]
   [:player/volume {:default 100 :persist true} any?]
   [:bg-player/show {:optional true :persist true} [:maybe boolean?]]
   [:bg-player/waiting {:optional true} [:maybe boolean?]]
   [:bg-player/loading {:optional true} [:maybe boolean?]]
   [:bg-player/ready {:optional true} [:maybe boolean?]]
   [:main-player/show {:optional true} [:maybe boolean?]]
   [:main-player/ready {:optional true} [:maybe boolean?]]
   [:search/results {:optional true} [:maybe vector?]]
   [:search/query {:optional true} [:maybe string?]]
   [:search/show-form {:optional true} [:maybe boolean?]]
   [:search/filter {:optional true} [:maybe map?]]
   [:search/show-suggestions {:optional true} [:maybe boolean?]]
   [:search/suggestions {:optional true} [:maybe map?]]
   [:layout/bg-overlay {:optional true} [:maybe map?]]
   [:layout/mobile-tooltip {:optional true} [:maybe map?]]
   [:layout/mobile-panel {:optional true} [:maybe map?]]
   [:layout/tooltips {:optional true} [:maybe map?]]
   [:layout/panels {:optional true} [:maybe map?]]
   [:auth/user {:optional true :persist true} [:maybe map?]]
   [:user/bookmarks {:optional true} any?]
   [:user/subscriptions {:optional true :persist true} any?]
   [:user/feed {:optional true :persist true} any?]
   [:user/feed-last-updated {:optional true :persist true} any?]
   [:peertube/instances {:default [] :persist true} [:vector PeerTubeInstance]]
   [:queue/show {:optional true} [:maybe boolean?]]
   [:queue/position {:default 0 :persist true} int?]
   [:queue/unshuffled {:optional true :persist true} [:maybe vector?]]
   [:queue {:default [] :persist true} vector?]
   [:notifications {:optional true} any?]
   [:modals {:optional true} any?]
   [:channel {:optional true} any?]
   [:stream {:optional true} any?]
   [:playlist {:optional true} any?]
   [:services {:optional true} [:maybe vector?]]
   [:service-id {:default 0 :persist true} int?]
   [:show-pagination-loading {:optional true} [:maybe boolean?]]
   [:kiosks {:optional true} any?]
   [:kiosk {:optional true} [:maybe map?]]
   [:available-kiosks {:optional true} [:maybe vector?]]
   [:bookmarks {:default [] :persist true} any?]
   [:subscriptions {:default [] :persist true} any?]
   [:feed {:optional true :persist true} any?]
   [:feed-last-updated {:optional true :persist true} any?]
   [:settings
    {:default (m/decode Settings {} transform/default-value-transformer)
     :persist true}
    Settings]])

(def default-local-db (m/decode LocalDB {} transform/default-value-transformer))

(def persisted-local-db-keys
  (->> LocalDB
       (m/children)
       (filter (comp :persist second))
       (map first)))

(def local-db-valid? (m/validator LocalDB))

(def local-db-explain (m/explainer LocalDB))
