(ns tubo.http
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [org.httpkit.server :refer [run-server]]
   [tubo.config :as config]
   [tubo.middleware :refer [reloading-ring-handler]]))

(defmethod ig/init-key :tubo/profile
  [_ profile]
  profile)

(defmethod ig/init-key ::service
  [_ {:keys [handler profile]}]
  (let [port (config/get-in [:backend :port])]
    (log/info "Starting HTTP server on port" port)
    (run-server (if (= profile :dev)
                  (reloading-ring-handler handler)
                  (handler))
                {:port port})))

(defmethod ig/halt-key! ::service
  [_ server]
  (server :timeout 100)
  (log/info "HTTP server stopped"))
