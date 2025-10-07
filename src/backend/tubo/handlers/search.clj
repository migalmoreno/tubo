(ns tubo.handlers.search
  (:require
   [clojure.java.data :as j]
   [clojure.string :as str]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.utils :as utils])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.search.SearchInfo))

(defn get-query-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [filter sort]}                           :query-params}]
  (.. (NewPipe/getService service-id)
      (getSearchQHFactory)
      (fromQuery q
                 (or (and (seq filter) (str/split filter #",")) '())
                 (or sort ""))))

(defn create-search-handler
  [{{{:keys [service-id]} :path} :parameters
    {:strs [nextPage]}           :query-params
    :as                          req}]
  (when-let [info (if nextPage
                    (SearchInfo/getMoreItems (NewPipe/getService service-id)
                                             (get-query-handler req)
                                             (utils/create-page nextPage))
                    (SearchInfo/getInfo (NewPipe/getService service-id)
                                        (get-query-handler req)))]
    (ok (utils/->ListInfo info req))))

(defn create-suggestions-handler
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters}]
  (when-let [extractor (.getSuggestionExtractor (NewPipe/getService
                                                 service-id))]
    (ok (j/from-java-shallow (.suggestionList extractor q) {}))))
