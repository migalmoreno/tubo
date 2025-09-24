(ns tubo.handlers.kiosks
  (:require
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.utils :as utils]
   [clojure.pprint :as pprint])
  (:import
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.localization.ContentCountry
   org.schabi.newpipe.extractor.NewPipe))

(defn get-default-kiosk
  [{{{:keys [service-id]} :path} :parameters
    {:strs [region]}             :query-params
    :as                          req}]
  (let [service   (NewPipe/getService service-id)
        extractor (doto (.getDefaultKioskExtractor
                         (if region
                           (doto (.getKioskList service)
                             (.forceContentCountry (ContentCountry. region)))
                           (.getKioskList service)))
                    (.fetchPage))
        info      (KioskInfo/getInfo extractor)]
    {:id              (.getId info)
     :name            (.getName info)
     :url             (.getUrl info)
     :service-id      service-id
     :next-page       (utils/get-next-page info)
     :related-streams (utils/get-items (.getRelatedItems info) req)}))

(defn get-kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [region]}                      :query-params
    :as                                   req}]
  (let [service (NewPipe/getService service-id)
        extractor
        (doto (.getExtractorById
               (if region
                 (doto (.getKioskList service)
                   (.forceContentCountry (ContentCountry. region)))
                 (.getKioskList service))
               kiosk-id
               nil)
          (.fetchPage))
        info (KioskInfo/getInfo extractor)]
    {:id              (.getId info)
     :name            (.getName info)
     :url             (.getUrl info)
     :service-id      service-id
     :next-page       (utils/get-next-page info)
     :related-streams (utils/get-items (.getRelatedItems info) req)}))

(defn get-kiosk-page
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [nextPage region]}             :query-params
    :as                                   req}]
  (let [service    (NewPipe/getService service-id)
        extractor  (.getExtractorById
                    (if region
                      (doto (.getKioskList service)
                        (.forceContentCountry (ContentCountry. region)))
                      (.getKioskList service))
                    kiosk-id
                    nil)
        kiosk-info (KioskInfo/getInfo extractor)
        info       (KioskInfo/getMoreItems service
                                           (.getUrl kiosk-info)
                                           (utils/create-page nextPage))]
    {:next-page       (utils/get-next-page info)
     :related-streams (utils/get-items (.getItems info) req)}))

(defn get-kiosks
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks  (.getKioskList service)]
    {:default-kiosk    (.getDefaultKioskId kiosks)
     :available-kiosks (.getAvailableKiosks kiosks)}))

(defn create-kiosks-handler
  [{{{:keys [service-id]} :path} :parameters}]
  (ok (get-kiosks service-id)))

(defn create-kiosk-handler
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [nextPage]}                    :query-params
    :as                                   req}]
  (ok (cond
        (and service-id nextPage kiosk-id) (get-kiosk-page req)
        (and service-id kiosk-id)          (get-kiosk req)
        :else                              (get-default-kiosk req))))
