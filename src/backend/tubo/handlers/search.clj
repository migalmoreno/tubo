(ns tubo.handlers.search
  (:require
   [clojure.java.data :as j]
   [clojure.string :as str]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.search.SearchInfo))

(defn get-suggestions
  [service-id query]
  (when-let [extractor (.getSuggestionExtractor
                        (NewPipe/getService service-id))]
    (j/from-java
     (.suggestionList extractor query))))

(defn get-search
  [service-id query {:keys [filter sort]}]
  (let [service (NewPipe/getService service-id)
        query-handler
        (.. service
            (getSearchQHFactory)
            (fromQuery query (or filter '()) (or sort "")))
        info (SearchInfo/getInfo service query-handler)]
    {:items             (utils/get-items (.getRelatedItems info))
     :next-page         (utils/get-next-page info)
     :service-id        service-id
     :search-suggestion (.getSearchSuggestion info)
     :corrected-search? (.isCorrectedSearch info)}))

(defn get-search-page
  [service-id query {:keys [filter sort]} next-page]
  (let [service       (NewPipe/getService service-id)
        query-handler (.. service
                          (getSearchQHFactory)
                          (fromQuery query (or filter '()) (or sort "")))
        info          (SearchInfo/getMoreItems service
                                               query-handler
                                               (utils/create-page next-page))]
    {:items     (utils/get-items (.getItems info))
     :next-page (utils/get-next-page info)}))

(defn create-search-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [filter sort nextPage]}                  :query-params}]
  (ok (if nextPage
        (get-search-page service-id
                         q
                         {:filter (and (seq filter) (str/split filter #","))
                          :sort   sort}
                         nextPage)
        (get-search service-id
                    q
                    {:filter (and (seq filter) (str/split filter #","))
                     :sort   sort}))))

(defn create-suggestions-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters}]
  (ok (get-suggestions service-id q)))
