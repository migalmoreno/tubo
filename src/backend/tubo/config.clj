(ns tubo.config
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :refer [resource]]
   [integrant.core :as ig]
   [malli.core :as m]
   [malli.error :as me]
   [tubo.schemas :as s]))

(def Config
  [:map {:closed true}
   [:backend/port int?]
   [:backend/url string?]
   [:registrations? boolean?]
   [:bg-helper/url {:optional true} string?]
   [:frontend/defaults s/Settings]
   [:peertube/default-instance {:optional true} s/PeerTubeInstance]
   [:youtube/consent-cookie? {:optional true} boolean?]])

(defmethod ig/init-key ::config
  [_ _]
  (let [config (dissoc (read-config (resource "config.edn")) :tubo/system)]
    (when-let [errors (m/explain Config config)]
      (throw (ex-info "Invalid config.edn" {:errors (me/humanize errors)})))
    config))
