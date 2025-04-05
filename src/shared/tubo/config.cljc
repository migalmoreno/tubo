(ns tubo.config
  (:require
   [aero.core :refer [read-config]]
   #?@(:clj [[clojure.java.io :refer [resource]]]
       :node [["path" :as path]])))

(defn config
  []
  (read-config #?(:clj (resource "config.edn")
                  :node (path/resolve "./resources/config.edn"))))

(defn bg-helper-url
  [config]
  (get-in config [:backend :bg-helper-url]))

(defn bg-helper-port
  [config]
  (get-in config [:bg-helper :port]))

(defn backend-port
  [config]
  (get-in config [:backend :port]))
