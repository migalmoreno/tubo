(ns tubo.handler
  (:require
   [clojure.string :as str]
   [ring.util.response :refer [response resource-response content-type]]
   [tubo.api :as api]))

(defn index
  [_]
  (-> (resource-response "index.html" {:root "public"})
      (content-type "text/html")))

(defn search
  [{{{:keys [service-id]} :path {:keys [q]} :query} :parameters
    {:strs [contentFilters sortFilter nextPage]}    :query-params}]
  (response (apply api/get-search
                   service-id
                   q
                   (and contentFilters (str/split contentFilters #","))
                   sortFilter
                   (if nextPage [nextPage] []))))

(defn channel
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply api/get-channel url (if nextPage [nextPage] []))))

(defn channel-tabs
  [{{:keys [url tab-id]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply api/get-channel-tab url tab-id (if nextPage [nextPage] []))))

(defn playlist
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply api/get-playlist url (if nextPage [nextPage] []))))

(defn comments
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (apply api/get-comments url (if nextPage [nextPage] []))))

(defn services
  [_]
  (response (api/get-services)))

(defn kiosks
  [{{{:keys [service-id]} :path} :parameters}]
  (response (api/get-kiosks service-id)))

(defn kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [nextPage region]}             :query-params}]
  (response (apply api/get-kiosk
                   {:region region}
                   (if kiosk-id
                     (into
                      [kiosk-id]
                      (into
                       (if service-id [service-id] [])
                       (if nextPage [nextPage] [])))
                     [service-id]))))

(defn stream
  [{{:keys [url]} :path-params}]
  (response (api/get-stream url)))
