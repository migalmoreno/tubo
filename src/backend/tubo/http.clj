(ns tubo.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [tubo.config :as config]
   [tubo.middleware :refer [reloading-ring-handler]]
   [tubo.router :as router]
   [clojure.tools.logging :as log]))

(defonce server (atom nil))

(defn start-server!
  [datasource]
  (let [port       (config/get-in [:backend :port])
        prod?      (System/getProperty "prod")
        handler-fn #(router/create-app-handler datasource)]
    (reset! server (run-server (if prod?
                                 (handler-fn)
                                 (reloading-ring-handler handler-fn))
                               {:port port}))
    (log/info "Backend server running on port" port)))

(defn stop-server! [] (when @server (@server :timeout 100) (reset! server nil)))
