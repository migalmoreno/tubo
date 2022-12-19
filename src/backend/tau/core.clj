(ns tau.core
  (:gen-class)
  (:require
   [tau.http :as http]))

(defn -main
  [& _]
  (http/start-server!))

(defn reset
  []
  (http/stop-server!))
