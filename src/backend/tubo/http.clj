(ns tubo.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [tubo.router :as router])
  (:import
   tubo.DownloaderImpl
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.Localization))

(defonce server (atom nil))

(defn start-server!
  ([] (start-server! 3000))
  ([port]
   (NewPipe/init (DownloaderImpl/init) (Localization. "en" "US"))
   (reset! server (run-server #'router/app {:port port}))
   (println "Server running in port" port)))

(defn stop-server! [] (when @server (@server :timeout 100) (reset! server nil)))
