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
  [{:keys [parameters] :as req}]
  (let [{:keys [service-id]}                         (:path parameters)
        {:keys [q]}                                  (:query parameters)
        {:strs [contentFilters sortFilter nextPage]} (:query-params req)
        content-filters                              (and contentFilters
                                                          (str/split
                                                           contentFilters
                                                           #","))]
    (response (if nextPage
                (api/get-search service-id q contentFilters sortFilter nextPage)
                (api/get-search service-id q contentFilters sortFilter)))))

(defn channel
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (api/get-channel url nextPage)
              (api/get-channel url))))

(defn playlist
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (api/get-playlist url nextPage)
              (api/get-playlist url))))

(defn comments
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (api/get-comments url nextPage)
              (api/get-comments url))))

(defn services
  [_]
  (response (api/get-services)))

(defn kiosks
  [{{{:keys [service-id]} :path} :parameters}]
  (response (api/get-kiosks service-id)))

(defn kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters
    {:strs [nextPage]}                    :query-params}]
  (response (cond
              (and kiosk-id service-id nextPage) (api/get-kiosk kiosk-id
                                                                service-id
                                                                nextPage)
              (and kiosk-id service-id)          (api/get-kiosk kiosk-id
                                                                service-id)
              :else                              (api/get-kiosk service-id))))

(defn stream
  [{{:keys [url]} :path-params}]
  (response (api/get-stream url)))
