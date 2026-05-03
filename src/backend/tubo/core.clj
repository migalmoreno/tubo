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
  (let [env    (keyword (or (System/getenv "TUBO_ENV") "dev"))
        system (ig/init (config env))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(halt-system system)))))
