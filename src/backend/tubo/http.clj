(ns tubo.http
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [org.httpkit.server :refer [run-server]]))

(defn start!
  [{:keys [config handler]}]
  (let [port (:api/port config)]
    (log/info "Starting HTTP server on port" port)
    (run-server (handler) {:port port})))

(defmethod ig/init-key ::api [_ opts] (start! opts))

(defmethod ig/halt-key! ::api
  [_ server]
  (server :timeout 100)
  (log/info "HTTP server stopped"))
