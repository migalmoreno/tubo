(ns tubo.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [tubo.config :as config]
   [tubo.downloader :as downloader]
   [tubo.potoken :as potoken]
   [tubo.router :as router])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.Localization
   org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor))

(defonce server (atom nil))

(defn start-server!
  ([]
   (start-server! (config/get-in [:backend :port])))
  ([port]
   (NewPipe/init (downloader/create-downloader-impl) (Localization. "en" "US"))
   (when (config/get-in [:backend :bg-helper-url])
     (YoutubeStreamExtractor/setPoTokenProvider
      (potoken/create-po-token-provider)))
   (reset! server (run-server #'router/app {:port port}))
   (println "Backend server running on port" port)))

(defn stop-server! [] (when @server (@server :timeout 100) (reset! server nil)))
