(ns tau.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.util.response :refer [response]]
   [reitit.ring :as ring]
   [reitit.coercion :as coercion]
   [reitit.ring.coercion :as rrc]
   [reitit.coercion.malli]
   [clojure.string :as str]
   [malli.experimental.lite :as l]
   [hiccup.page :as hiccup]
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

(defn index-html
  []
  (hiccup/html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title "Tau"]
    (hiccup/include-css "/css/output.css")]
   [:body
    [:div#app]
    (hiccup/include-js "/js/main.js")
    [:script "tau.core.init();"]]))

(defonce server (atom nil))

(def router
  (ring/router
   [["/" (constantly (response (index-html)))]
    ["/search" (constantly (response (index-html)))]
    ["/stream" (constantly (response (index-html)))]
    ["/channel" (constantly (response (index-html)))]
    ["/api"
     ["/stream" {:get (fn [{{:strs [url]} :query-params}]
                        (response (stream/get-info url)))}]
     ["/search" {:get {:coercion reitit.coercion.malli/coercion
                       :parameters {:query {:serviceId int?
                                            :q string?
                                            ;; :sortFilter (l/maybe string?)
                                            ;; :contentFilters (l/maybe string?)
                                            ;; :nextPage (l/maybe string?)
                                            }}
                       :handler (fn [{:keys [parameters]}]
                                  (let [{:keys [contentFilters serviceId q sortFilter nextPage]} (:query parameters)
                                        content-filters (and contentFilters (str/split contentFilters #","))]
                                    (response (apply search/get-info serviceId q
                                                     content-filters sortFilter
                                                     (or nextPage '())))))}}]
     ["/channel" {:get (fn [{{:keys [url nextPage]} :query-params}]
                         (response (channel/get-info url nextPage)))}]
     ["/playlist" {:get (fn [{{:keys [url nextPage]} :query-params}]
                          (response (playlist/get-info url nextPage)))}]
     ["/comments" {:get (fn [{{:keys [url nextPage]} :query-params}]
                          (response (apply comment/get-info url (or nextPage '()))))}]
     ["/services" {:get (constantly (response (service/get-services)))}]
     ["/kiosks"
      ["" {:coercion reitit.coercion.malli/coercion
           :parameters {:query {:serviceId int?}}
           :get (fn [{:keys [parameters]}]
                  (println parameters)
                  (response (kiosk/get-kiosks (-> parameters :query :serviceId))))}]
      ["/:kioskId" {:get {:coercion reitit.coercion.malli/coercion
                          :parameters {:query {:serviceId int?}}
                          :handler (fn [{{:keys [kioskId serviceId nextPage]} :query-params}]
                                     (response (kiosk/get-info kioskId serviceId nextPage)))}}]]]]
   ;;{:compile coercion/compile-request-coercers}
   {:data {:middleware [rrc/coerce-request-middleware
                        rrc/coerce-response-middleware
                        rrc/coerce-exceptions-middleware]}}))

(def app
  (ring/ring-handler
   router
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404, :body "Not found"})}))
   {:middleware [wrap-params
                 [wrap-json-response {:pretty true}]
                 wrap-reload]}))

(defn stop-server!
  []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server!
  ([]
   (start-server! 3000))
  ([port]
   (NewPipe/init (DownloaderImpl/init) (Localization. "en" "GB"))
   (reset! server (run-server #'app {:port port}))
   (println "Server running in port" port)))
