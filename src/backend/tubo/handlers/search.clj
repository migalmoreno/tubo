(ns tubo.handlers.search
  (:require
   [clojure.java.data :refer [from-java]]
   [clojure.string :as str]
   [ring.util.response :refer [response]]
   [ring.util.codec :refer [url-decode]]
   [tubo.handlers.utils :refer [get-items]])
  (:import
   org.schabi.newpipe.extractor.search.SearchInfo
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.Page))

(defn get-suggestions
  [service-id query]
  (when-let [extractor (.getSuggestionExtractor
                        (NewPipe/getService service-id))]
    (from-java
     (.suggestionList extractor query))))

(defn get-search
  ([service-id query {:keys [filter sort]}]
   (let [service (NewPipe/getService service-id)
         query-handler
         (.. service
             (getSearchQHFactory)
             (fromQuery query (or filter '()) (or sort "")))
         info (SearchInfo/getInfo service query-handler)]
     {:items             (get-items (.getRelatedItems info))
      :next-page         (from-java (.getNextPage info))
      :service-id        service-id
      :search-suggestion (.getSearchSuggestion info)
      :corrected-search? (.isCorrectedSearch info)}))
  ([service-id query {:keys [filter sort]} page-url]
   (let [service       (NewPipe/getService service-id)
         query-handler (.. service
                           (getSearchQHFactory)
                           (fromQuery query (or filter '()) (or sort "")))
         search-info   (SearchInfo/getInfo service query-handler)
         next-page     (.getNextPage search-info)
         info          (SearchInfo/getMoreItems service
                                                query-handler
                                                (Page. (url-decode page-url)
                                                       (.getId next-page)
                                                       (.getIds next-page)
                                                       (.getCookies next-page)
                                                       (.getBody next-page)))]
     {:items     (get-items (.getItems info))
      :next-page (from-java (.getNextPage info))})))

(defn create-search-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [filter sort nextPage]}                  :query-params}]
  (response (apply get-search
                   service-id
                   q
                   {:filter (and (seq filter) (str/split filter #","))
                    :sort   sort}
                   (if nextPage [nextPage] []))))

(defn create-suggestions-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters}]
  (response (get-suggestions service-id q)))
