(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :refer [clear go halt prep init reset reset-all] :as repl]
   [ring.middleware.reload :as reload]
   [portal.api :as p]
   [tubo.http :as http]
   [tubo.system :refer [config]]))

(add-tap #'p/submit)

(repl/set-prep! #(ig/expand (config :dev)))

(defn reloading-ring-handler
  [f]
  (let [reload! (#'reload/reloader ["src/backend" "src/shared"] true)]
    (fn
      ([request]
       (reload!)
       ((f) request))
      ([request respond raise]
       (reload!)
       ((f) request respond raise)))))

(defmethod ig/init-key :tubo.http/api
  [_ {:keys [handler] :as opts}]
  (http/start! (assoc opts :handler #(reloading-ring-handler handler))))
