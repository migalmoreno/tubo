(ns tau.api.kiosk
  (:require
   [clojure.java.data :as j]
   [tau.api.stream :as stream]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.StreamingService
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.NewPipe))

(defrecord KioskList
    [default-kiosk available-kiosks])

(defrecord Kiosk
    [id url next-page related-streams])

(defrecord KioskPage
    [next-page related-streams])

(defn get-kiosk-info
  ([kiosk-id service-id]
   (let [service (NewPipe/getService service-id)
         extractor (.getExtractorById (.getKioskList service) kiosk-id nil)
         info (KioskInfo/getInfo extractor)]
     (map->Kiosk
      {:id (.getId info)
       :url (.getUrl info)
       :next-page (j/from-java (.getNextPage info))
       :related-streams (map #(stream/get-stream-result %) (.getRelatedItems info))})))
  ([kiosk-id service-id page-url]
   (let  [service (NewPipe/getService service-id)
          extractor (.getExtractorById (.getKioskList service) kiosk-id nil)
          url (url-decode page-url)
          kiosk-info (KioskInfo/getInfo extractor)
          info (KioskInfo/getMoreItems service (.getUrl kiosk-info) (Page. url))]
     (map->KioskPage
      {:next-page (j/from-java (.getNextPage info))
       :related-streams (map #(stream/get-stream-result %) (.getItems info))}))))

(defn get-kiosk-list-info
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks (.getKioskList service)]
    (map->KioskList
     {:default-kiosk (.getDefaultKioskId kiosks)
      :available-kiosks (.getAvailableKiosks kiosks)})))
