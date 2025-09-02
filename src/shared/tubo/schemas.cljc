(ns tubo.schemas
  (:require
   [tubo.config :as config]
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
   [:duration int?]
   [:uploader-avatar [:maybe uri?]]
   [:thumbnail [:maybe uri?]]
   [:uploader-url uri?]
   [:uploader-verified? boolean?]
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
   [:url uri?]])

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

(def Theme [:enum "auto" "light" "dark"])

(def ItemLayout [:enum "list" "grid"])

(def ImageQuality [:enum :high :medium :low :none])

(def Settings
  [:map {:closed true}
   [:theme {:default "auto"} Theme]
   [:show-comments {:default true} boolean?]
   [:show-related {:default true} boolean?]
   [:show-description {:default true} boolean?]
   [:items-layout {:default "list"} ItemLayout]
   [:default-resolution {:default "720p"} string?]
   [:default-video-format {:default "MPEG-4"} string?]
   [:default-audio-format {:default "m4a"} string?]
   [:instance {:default (config/get-in [:frontend :api-url])} uri?]
   [:auth-instance {:default (config/get-in [:frontend :auth-url])} uri?]
   [:image-quality {:default :high} ImageQuality]
   [:default-country {:default {0 {:name "United States" :code "US"}}} any?]
   [:default-kiosk {:default {0 "Trending"}} any?]
   [:default-filter {:default {0 "all"}} any?]
   [:default-service {:default 0} int?]])

(def LocalDB
  [:map {:closed true}
   [:player/paused {:default true :persist true} boolean?]
   [:player/muted {:optional true :persist true} [:maybe boolean?]]
   [:player/shuffled {:optional true :persist true} [:maybe boolean?]]
   [:player/loop {:default true :persist true} [:maybe boolean?]]
   [:player/volume {:default 100 :persist true} int?]
   [:bg-player/show {:optional true :persist true} [:maybe boolean?]]
   [:queue {:default [] :persist true} vector?]
   [:queue/position {:default 0 :persist true} int?]
   [:queue/unshuffled {:optional true :persist true} vector?]
   [:service-id {:default 0 :persist true} int?]
   [:auth/user {:optional true :persist true} any?]
   [:peertube/instances
    {:default [(config/get-in [:services :peertube :default-instance])]
     :persist
     true} [:vector PeerTubeInstance]]
   [:bookmarks {:default [] :persist true} any?]
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
