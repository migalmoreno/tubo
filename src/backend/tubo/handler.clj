(ns tubo.handler
  (:require
   [clojure.string :as str]
   [hiccup.page :as hiccup]
   [ring.util.response :refer [response]]
   [tubo.api :as api]))

(defn index
  [_]
  (response
   (hiccup/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta
      {:name "viewport"
       :content
       "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
     [:title "Tubo"]
     [:link {:rel "icon" :type "image/svg+xml" :href "/icons/tubo.svg"}]
     (hiccup/include-css "/styles/index.css")]
    [:body
     [:div#app]
     (hiccup/include-js "/js/main.js")
     [:script "tubo.core.init();"]])))

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
    {:strs [nextPage]}                    :query-params}]
  (response (apply api/get-kiosk
                   kiosk-id
                   (into (if service-id [service-id] [])
                         (if nextPage [nextPage] [])))))

(defn stream
  [{{:keys [url]} :path-params}]
  (response (api/get-stream url)))
