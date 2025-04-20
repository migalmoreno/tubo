(ns tubo.middlewares
  (:require
   [ring.middleware.reload :as reload]))

(defn reloading-ring-handler
  [f]
  (let [reload! (#'reload/reloader ["src"] true)]
    (fn
      ([request]
       (reload!)
       ((f) request))
      ([request respond raise]
       (reload!)
       ((f) request respond raise)))))
