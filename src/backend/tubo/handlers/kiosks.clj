(ns tubo.handlers.kiosks
  (:require
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.localization.ContentCountry
   org.schabi.newpipe.extractor.NewPipe))

(defn get-kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [region]}                      :query-params}]
  (let [service    (NewPipe/getService service-id)
        kiosk-list (if region
                     (doto (.getKioskList service)
                       (.forceContentCountry (ContentCountry. region)))
                     (.getKioskList service))]
    (-> (doto (if (and kiosk-id service-id)
                (.getExtractorById kiosk-list kiosk-id nil)
                (.getDefaultKioskExtractor kiosk-list))
              (.fetchPage))
        KioskInfo/getInfo)))

(defn create-kiosks-handler
  [{{{:keys [service-id]} :path} :parameters}]
  (ok (utils/->Info (.getKioskList (NewPipe/getService service-id)))))

(defn create-kiosk-handler
  [{{{:keys [service-id]} :path} :parameters
    {:strs [nextPage]}           :query-params
    :as                          req}]
  (when-let [info (if nextPage
                    (KioskInfo/getMoreItems (NewPipe/getService service-id)
                                            (.getUrl (get-kiosk req))
                                            (utils/create-page nextPage))
                    (get-kiosk req))]
    (ok (utils/->ListInfo info req))))
