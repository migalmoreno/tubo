(ns tau.api.search
  (:require
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-encode url-decode]]
   [tau.api.result :as result])
  (:import
   org.schabi.newpipe.extractor.search.SearchInfo
   org.schabi.newpipe.extractor.InfoItem
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defrecord SearchResult
    [items next-page search-suggestion corrected-search?])

(defrecord SearchResultPage
    [items next-page])

(defn get-info
  ([service-id query content-filters sort-filter]
   (let [service (NewPipe/getService service-id)
         query-handler (.. service
                           (getSearchQHFactory)
                           (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getInfo service query-handler)]
     (map->SearchResult
      {:items (result/get-results (.getRelatedItems info))
       :next-page (j/from-java (.getNextPage info))
       :search-suggestion (.getSearchSuggestion info)
       :corrected-search? (.isCorrectedSearch info)})))
  ([service-id query content-filters sort-filter page-url]
   (let [service (NewPipe/getService service-id)
         url (url-decode page-url)
         query-handler (.. service
                           (getSearchQHFactory)
                           (fromQuery query (or content-filters '()) (or sort-filter "")))
         info (SearchInfo/getMoreItems service query-handler (Page. url))]
     (map->SearchResultPage
      {:items (result/get-results (.getItems info))
       :next-page (j/from-java (.getNextPage info))}))))
