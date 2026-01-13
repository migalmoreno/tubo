(ns tubo.core
  (:gen-class)
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [integrant.core :as ig]
   [tubo.system :refer [config]]))

(defn halt-system
  [system]
  (ig/halt! system))

(def cli-options
  [["-e" "--env ENV" "Environment name"
    :default :dev]])

(defn -main
  [& args]
  (let [parsed-opts (parse-opts args cli-options)
        system      (ig/init (config (or (some-> (get-in parsed-opts
                                                         [:options :env])
                                                 keyword)
                                         :dev)))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(halt-system system)))))
