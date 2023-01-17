(ns tubo.core
  (:gen-class)
  (:require
   [tubo.http :as http]))

(defn -main
  [& _]
  (http/start-server!))

(defn reset
  []
  (http/stop-server!))
