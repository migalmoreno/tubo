(ns tubo.handler
  (:require
   [clojure.string :as str]
   [hiccup.page :as hiccup]
   [ring.util.response :refer [response]]
   [tubo.api.streams :as streams]
   [tubo.api.channels :as channels]
   [tubo.api.playlists :as playlists]
   [tubo.api.comments :as comments]
   [tubo.api.services :as services]))

(defn index
  [_]
  (response
   (hiccup/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title "Tubo"]
     [:link {:rel "icon" :type "image/png" :href "/images/tubo.png"}]
     (hiccup/include-css "/css/tubo.css")]
    [:body
     [:div#app]
     (hiccup/include-js "/js/main.js")
     [:script "tubo.core.init();"]])))

(defn search
  [{:keys [parameters] :as req}]
  (let [{:keys [service-id]} (:path parameters)
        {:keys [q]} (:query parameters)
        {:strs [contentFilters sortFilter nextPage]} (:query-params req)
        content-filters (and contentFilters (str/split contentFilters #","))]
    (response (if nextPage
                (services/search service-id q contentFilters sortFilter nextPage)
                (services/search service-id q contentFilters sortFilter)))))

(defn channel
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (channels/get-channel url nextPage)
              (channels/get-channel url))))

(defn playlist
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (playlists/get-playlist url nextPage)
              (playlists/get-playlist url))))

(defn comments
  [{{:keys [url]} :path-params {:strs [nextPage]} :query-params}]
  (response (if nextPage
              (comments/get-comments url nextPage)
              (comments/get-comments url))))

(defn services
  [_]
  (response (services/get-services)))

(defn kiosks
  [{{{:keys [service-id]} :path} :parameters}]
  (response (services/get-kiosks service-id)))

(defn kiosk
  [{{{:keys [kiosk-id service-id]} :path} :parameters {:strs [nextPage]} :query-params}]
  (response (cond
              (and kiosk-id service-id nextPage) (services/get-kiosk kiosk-id service-id nextPage)
              (and kiosk-id service-id) (services/get-kiosk kiosk-id service-id)
              :else (services/get-kiosk service-id))))

(defn stream [{{:keys [url]} :path-params}]
  (response (streams/get-stream url)))
