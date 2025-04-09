(ns tubo.handlers.kiosks
  (:require
   [clojure.java.data :as j]
   [ring.util.response :refer [response]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :refer [get-items]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.localization.ContentCountry
   org.schabi.newpipe.extractor.kiosk.KioskInfo))

(defn get-kiosk
  ([{:keys [region]} service-id]
   (let [service   (NewPipe/getService service-id)
         extractor (doto (.getDefaultKioskExtractor
                          (if region
                            (doto (.getKioskList service)
                              (.forceContentCountry (ContentCountry. region)))
                            (.getKioskList service)))
                     (.fetchPage))
         info      (KioskInfo/getInfo extractor)]
     {:id              (.getId info)
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([{:keys [region]} kiosk-id service-id]
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
      :url             (.getUrl info)
      :service-id      service-id
      :next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getRelatedItems info))}))
  ([{:keys [region]} kiosk-id service-id page-url]
   (let [service    (NewPipe/getService service-id)
         extractor  (.getExtractorById
                     (if region
                       (doto (.getKioskList service)
                         (.forceContentCountry (ContentCountry. region)))
                       (.getKioskList service))
                     kiosk-id
                     nil)
         url        (url-decode page-url)
         kiosk-info (KioskInfo/getInfo extractor)
         info       (KioskInfo/getMoreItems service
                                            (.getUrl kiosk-info)
                                            (Page. url))]
     {:next-page       (j/from-java (.getNextPage info))
      :related-streams (get-items (.getItems info))})))

(defn get-kiosks
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks  (.getKioskList service)]
    {:default-kiosk    (.getDefaultKioskId kiosks)
     :available-kiosks (.getAvailableKiosks kiosks)}))

(defn create-kiosks-handler
  [{{{:keys [service-id]} :path} :parameters}]
  (response (get-kiosks service-id)))

(defn create-kiosk-handler
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [nextPage region]}             :query-params}]
  (response (apply get-kiosk
                   {:region region}
                   (if kiosk-id
                     (into
                      [kiosk-id]
                      (into
                       (if service-id [service-id] [])
                       (if nextPage [nextPage] [])))
                     [service-id]))))
