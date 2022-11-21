(ns tau.services.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]
   [compojure.route :as route]
   [compojure.core :refer :all]
   [compojure.coercions :refer [as-int]]
   [clojure.string :as str]
   [tau.api.stream :as stream]
   [tau.api.search :as search]
   [tau.api.channel :as channel]
   [tau.api.playlist :as playlist]
   [tau.api.comment :as comment]
   [tau.api.kiosk :as kiosk]
   [tau.api.service :as service])
  (:import
   tau.DownloaderImpl
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.localization.Localization))

(defonce server (atom nil))

(defn stop-server!
  []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))

(defroutes app-routes
  (context "/api" []
           (GET "/stream" [url]
                (response (stream/get-stream-info url)))
           (GET "/search" [serviceId :<< as-int q sortFilter contentFilters nextPage]
                (let [content-filters (when contentFilters (str/split contentFilters #","))]
                  (response (if nextPage
                              (search/get-search-info serviceId q content-filters sortFilter nextPage)
                              (search/get-search-info serviceId q content-filters sortFilter)))))
           (GET "/channel" [url nextPage]
                (if nextPage
                  (response (channel/get-channel-info url nextPage))
                  (response (channel/get-channel-info url))))
           (GET "/playlist" [url nextPage]
                (if nextPage
                  (response (playlist/get-playlist-info url nextPage))
                  (response (playlist/get-playlist-info url))))
           (GET "/comments" [url nextPage]
                (if nextPage
                  (response (comment/get-comments-info url nextPage))
                  (response (comment/get-comments-info url))))
           (GET "/services" []
                (response (service/get-service-list-info)))
           (context "/kiosks" []
                    (GET "/" [serviceId :<< as-int]
                         (response (kiosk/get-kiosk-list-info serviceId)))
                    (GET "/:kioskId" [kioskId serviceId :<< as-int nextPage]
                         (if nextPage
                           (response (kiosk/get-kiosk-info kioskId serviceId nextPage))
                           (response (kiosk/get-kiosk-info kioskId serviceId)))))))

(defn make-handler
  []
  (-> #'app-routes
      wrap-params
      (wrap-json-response {:pretty true})
      wrap-reload))

(defn start-server!
  [port]
  (NewPipe/init (DownloaderImpl/init) (Localization. "en" "GB"))
  (reset! server (run-server (make-handler) {:port port})))
