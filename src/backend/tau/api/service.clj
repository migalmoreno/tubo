(ns tau.api.service
  (:require
   [clojure.java.data :as j]
   [tau.api.kiosk :as kiosk])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.kiosk.KioskList
   org.schabi.newpipe.extractor.StreamingService))

(defrecord Service
    [id info base-url kiosk-list])

(defn get-service-info
  [service]
  (map->Service
   {:id (.getServiceId service)
    :info (j/from-java (.getServiceInfo service))
    :base-url (.getBaseUrl service)
    :kiosk-list (map #(kiosk/get-kiosk-info % (.getServiceId service))
                     (.getAvailableKiosks (.getKioskList service)))}))

(defn get-service-list-info
  []
  (map #(get-service-info %) (NewPipe/getServices)))
