(ns tubo.api.services
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-encode url-decode]]
   [tubo.api.items :as items])
  (:import
   org.schabi.newpipe.extractor.kiosk.KioskInfo
   org.schabi.newpipe.extractor.kiosk.KioskList
   org.schabi.newpipe.extractor.InfoItem
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page
   org.schabi.newpipe.extractor.StreamingService
   org.schabi.newpipe.extractor.search.SearchInfo))

(defn search
  ([service-id query content-filters sort-filter]
   (let [service (NewPipe/getService service-id)
         query-handler (.. service
                           (getSearchQHFactory)
                           (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getInfo service query-handler)]
     {:items (items/get-items (.getRelatedItems info))
      :next-page (j/from-java (.getNextPage info))
      :search-suggestion (.getSearchSuggestion info)
      :corrected-search? (.isCorrectedSearch info)}))
  ([service-id query content-filters sort-filter page-url]
   (let [service (NewPipe/getService service-id)
         url (url-decode page-url)
         query-handler (.. service
                           (getSearchQHFactory)
                           (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getMoreItems service query-handler (Page. url))]
     {:items (items/get-items (.getItems info))
      :next-page (j/from-java (.getNextPage info))})))

(defn get-kiosk
  ([service-id]
   (let [service (NewPipe/getService service-id)
         extractor (doto (.getDefaultKioskExtractor (.getKioskList service))
                     (.fetchPage))
         info (KioskInfo/getInfo extractor)]
     {:id (.getId info)
      :url (.getUrl info)
      :next-page (j/from-java (.getNextPage info))
      :related-streams (items/get-items (.getRelatedItems info))}))
  ([kiosk-id service-id]
   (let [service (NewPipe/getService service-id)
         extractor (doto (.getExtractorById (.getKioskList service) kiosk-id nil)
                     (.fetchPage))
         info (KioskInfo/getInfo extractor)]
     {:id (.getId info)
      :url (.getUrl info)
      :next-page (j/from-java (.getNextPage info))
      :related-streams (items/get-items (.getRelatedItems info))}))
  ([kiosk-id service-id page-url]
   (let  [service (NewPipe/getService service-id)
          extractor (.getExtractorById (.getKioskList service) kiosk-id nil)
          url (url-decode page-url)
          kiosk-info (KioskInfo/getInfo extractor)
          info (KioskInfo/getMoreItems service (.getUrl kiosk-info) (Page. url))]
     {:next-page (j/from-java (.getNextPage info))
      :related-streams (items/get-items (.getItems info))})))

(defn get-kiosks
  [service-id]
  (let [service (NewPipe/getService service-id)
        kiosks (.getKioskList service)]
    {:default-kiosk (.getDefaultKioskId kiosks)
     :available-kiosks (.getAvailableKiosks kiosks)}))

(defn get-service
  [service]
  {:id (.getServiceId service)
   :info (j/from-java (.getServiceInfo service))
   :base-url (.getBaseUrl service)})

(defn get-services
  []
  (map get-service (NewPipe/getServices)))
