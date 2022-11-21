(ns tau.core
  (:require
   [tau.services.http :as http]))

(defn -main
  [& _]
  (http/start-server! 3000))

(defn reset
  []
  (http/stop-server!))
