(ns tubo.system
  (:require
   [aero.core :as aero]
   [integrant.core :as ig]
   [clojure.java.io :refer [resource]]
   tubo.config
   tubo.db
   tubo.extractors.newpipe
   tubo.router
   tubo.http))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn config
  [profile]
  (:tubo/system (aero/read-config (resource "config.edn") {:profile profile})))
