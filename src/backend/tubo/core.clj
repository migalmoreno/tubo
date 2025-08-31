(ns tubo.core
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [tubo.system :refer [config]]))

(defn halt-system
  [system]
  (ig/halt! system))

(defn -main
  [& _]
  (let [system (ig/init config)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(halt-system system)))))
