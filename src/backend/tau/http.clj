(ns tau.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [tau.router :as router])
  (:import
   tau.DownloaderImpl
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.Localization))

(defonce server (atom nil))

(defn start-server!
  ([]
   (start-server! 3000))
  ([port]
   (NewPipe/init (DownloaderImpl/init) (Localization. "en" "GB"))
   (reset! server (run-server #'router/app {:port port}))
   (println "Server running in port" port)))

(defn stop-server!
  []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))
