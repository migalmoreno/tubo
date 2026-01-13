(ns tubo.system
  (:require
   [aero.core :as aero]
   [integrant.core :as ig]
   [clojure.java.io :refer [resource]]
   tubo.db
   tubo.downloader
   tubo.router
   tubo.http))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn config
  [profile]
  (aero/read-config (resource "system.edn") {:profile profile}))
