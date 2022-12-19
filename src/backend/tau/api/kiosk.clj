(ns tau.api.kiosk
  (:require
   [clojure.java.data :as j]
   [tau.api.stream :as stream]
   [tau.api.channel :as channel]
   [tau.api.playlist :as playlist]
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

(defn get-results
  [items]
  (map #(case (.name (.getInfoType %))
          "STREAM" (stream/get-result %)
          "CHANNEL" (channel/get-result %)
          "PLAYLIST" (playlist/get-result %))
       items))

(defn get-info
  ([kiosk-id service-id]
   (let [service (NewPipe/getService service-id)
         extractor (.getExtractorById (.getKioskList service) kiosk-id nil)
         info (KioskInfo/getInfo extractor)]
     (map->Kiosk
      {:id (.getId info)
       :url (.getUrl info)
       :next-page (j/from-java (.getNextPage info))
       :related-streams (get-results (.getRelatedItems info))})))
  ([kiosk-id service-id page-url]
   (let  [service (NewPipe/getService service-id)
          extractor (.getExtractorById (.getKioskList service) kiosk-id nil)
          url (url-decode page-url)
          kiosk-info (KioskInfo/getInfo extractor)
          info (KioskInfo/getMoreItems service (.getUrl kiosk-info) (Page. url))]
     (map->KioskPage
      {:next-page (j/from-java (.getNextPage info))
       :related-streams (get-results (.getItems info))}))))

(defn get-kiosks
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks (.getKioskList service)]
    (map->KioskList
     {:default-kiosk (.getDefaultKioskId kiosks)
      :available-kiosks (.getAvailableKiosks kiosks)})))
