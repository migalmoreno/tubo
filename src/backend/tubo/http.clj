(ns tubo.http
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [org.httpkit.server :refer [run-server]]
   [tubo.config :as config]
   [tubo.middleware :refer [reloading-ring-handler]]))

(defmethod ig/init-key ::service
  [_ {:keys [handler]}]
  (let [prod? (System/getProperty "prod")
        port  (config/get-in [:backend :port])]
    (log/info "Starting HTTP server on port" port)
    (run-server (if prod?
                  (handler)
                  (reloading-ring-handler handler))
                {:port port})))

(defmethod ig/halt-key! ::service
  [_ server]
  (do
    (server :timeout 100)
    (log/info "HTTP server stopped")))
