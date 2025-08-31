(ns tubo.system
  #?(:clj (:require
           [integrant.core :as ig]
           [clojure.java.io :refer [resource]]
           tubo.db
           tubo.downloader
           tubo.router
           tubo.http)))

#?(:clj (def config (ig/read-string (slurp (resource "system.edn")))))
