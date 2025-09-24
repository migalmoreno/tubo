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
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [filter sort]}                           :query-params
    :as                                             req}]
  (let [service (NewPipe/getService service-id)
        query-handler
        (.. service
            (getSearchQHFactory)
            (fromQuery q
                       (or (and (seq filter) (str/split filter #",")) '())
                       (or sort "")))
        info (SearchInfo/getInfo service query-handler)]
    {:items             (utils/get-items (.getRelatedItems info) req)
     :next-page         (utils/get-next-page info)
     :service-id        service-id
     :search-suggestion (.getSearchSuggestion info)
     :corrected-search? (.isCorrectedSearch info)}))

(defn get-search-page
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [filter sort nextPage]}                  :query-params
    :as                                             req}]
  (let [service       (NewPipe/getService service-id)
        query-handler (.. service
                          (getSearchQHFactory)
                          (fromQuery
                           q
                           (or (and (seq filter) (str/split filter #",")) '())
                           (or sort "")))
        info          (SearchInfo/getMoreItems service
                                               query-handler
                                               (utils/create-page nextPage))]
    {:items     (utils/get-items (.getItems info) req)
     :next-page (utils/get-next-page info)}))

(defn create-search-handler
  [{{:strs [nextPage]} :query-params :as req}]
  (ok (if nextPage (get-search-page req) (get-search req))))

(defn create-suggestions-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters}]
  (ok (get-suggestions service-id q)))
