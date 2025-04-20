(ns tubo.core
  (:gen-class)
  (:require
   [migratus.core :as migratus]
   [next.jdbc.connection :as connection]
   [tubo.config :as config]
   [tubo.downloader :as downloader]
   [tubo.http :as http]
   [tubo.potoken :as potoken])
  (:import
   com.zaxxer.hikari.HikariDataSource
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.Localization
   org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
   org.schabi.newpipe.extractor.ServiceList
   org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor))

(defn start-npe
  []
  (NewPipe/init (downloader/create-downloader-impl) (Localization. "en" "US"))
  (when (config/get-in [:backend :bg-helper-url])
    (YoutubeStreamExtractor/setPoTokenProvider
     (potoken/create-po-token-provider)))
  (when-let [instance (first (config/get-in [:peertube :instances]))]
    (.setInstance ServiceList/PeerTube
                  (PeertubeInstance. (:url instance) (:name instance)))))

(defn -main
  [& _]
  (let [ds (connection/->pool HikariDataSource (config/get-in [:backend :db]))]
    (start-npe)
    (migratus/migrate {:store :database :db {:datasource ds}})
    (http/start-server! ds)))

(defn reset [] (http/stop-server!))
