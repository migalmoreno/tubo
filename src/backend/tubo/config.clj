(ns tubo.config
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :refer [resource]]
   [integrant.core :as ig]))

(defmethod ig/init-key ::config
  [_ _]
  (dissoc (read-config (resource "config.edn")) :tubo/system))
