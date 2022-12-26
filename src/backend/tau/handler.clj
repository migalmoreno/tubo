(ns tau.handler
  (:require
   [clojure.string :as str]
   [hiccup.page :as hiccup]
   [ring.util.response :refer [response]]
   [tau.api.stream :as stream]
   [tau.api.search :as search]
   [tau.api.channel :as channel]
   [tau.api.playlist :as playlist]
   [tau.api.comment :as comment]
   [tau.api.kiosk :as kiosk]
   [tau.api.service :as service]))

(defn index
  [_]
  (response
   (hiccup/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title "Tau"]
     (hiccup/include-css "/css/tau.css")]
    [:body
     [:div#app]
     (hiccup/include-js "/js/main.js")
     [:script "tau.core.init();"]])))

(defn search
  [{:keys [parameters] :as req}]
  (let [{:keys [service-id]} (:path parameters)
        {:keys [q]} (:query parameters)
        {:strs [contentFilters sortFilter nextPage]} (:query-params req)
        content-filters (and contentFilters (str/split contentFilters #","))]
    (response (if nextPage
                (search/get-info service-id q contentFilters sortFilter nextPage)
                (search/get-info service-id q contentFilters sortFilter)))))

(defn channel
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (channel/get-info url nextPage)
              (channel/get-info url))))

(defn playlist
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (playlist/get-info url nextPage)
              (playlist/get-info url))))

(defn comments
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (comment/get-info url nextPage)
              (comment/get-info url))))

(defn services
  [_]
  (response (service/get-services)))

(defn kiosks
  [{{{:keys [service-id]} :path} :parameters}]
  (response (kiosk/get-kiosks service-id)))

(defn kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (kiosk/get-info kiosk-id service-id nextPage)
              (kiosk/get-info kiosk-id service-id))))

(defn stream [{{:keys [url]} :path-params}]
  (response (stream/get-info url)))
