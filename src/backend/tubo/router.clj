(ns tubo.router
  (:require
   [muuntaja.core :as m]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.coercion.malli]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as res]
   [tubo.handlers.channel :as channel]
   [tubo.handlers.comments :as comments]
   [tubo.handlers.kiosks :as kiosks]
   [tubo.handlers.playlist :as playlist]
   [tubo.handlers.search :as search]
   [tubo.handlers.services :as services]
   [tubo.handlers.stream :as stream]
   [tubo.routes :as routes]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (case data
      :api/services {:get {:summary "returns all supported services"
                           :handler services/create-services-handler}}
      :api/search {:get {:summary
                         "returns search results for a given service"
                         :coercion reitit.coercion.malli/coercion
                         :parameters {:path  {:service-id int?}
                                      :query {:q string?}}
                         :handler search/create-search-handler}}
      :api/suggestions {:get {:summary
                              "returns search suggestions for a given service"
                              :coercion reitit.coercion.malli/coercion
                              :parameters {:path  {:service-id int?}
                                           :query {:q string?}}
                              :handler search/create-suggestions-handler}}
      :api/default-kiosk {:get
                          {:summary
                           "returns default kiosk entries for a given service"
                           :coercion reitit.coercion.malli/coercion
                           :parameters {:path {:service-id int?}}
                           :handler kiosks/create-kiosk-handler}}
      :api/all-kiosks {:get {:summary
                             "returns all kiosks supported by a given service"
                             :coercion reitit.coercion.malli/coercion
                             :parameters {:path {:service-id int?}}
                             :handler kiosks/create-kiosks-handler}}
      :api/kiosk {:get
                  {:summary
                   "returns kiosk entries for a given service and a kiosk ID"
                   :coercion reitit.coercion.malli/coercion
                   :parameters {:path {:service-id int? :kiosk-id string?}}
                   :handler kiosks/create-kiosk-handler}}
      :api/stream {:get {:summary "returns stream data for a given URL"
                         :handler stream/create-stream-handler}}
      :api/channel {:get {:summary "returns channel data for a given URL"
                          :handler channel/create-channel-handler}}
      :api/channel-tab {:get
                        {:summary
                         "returns channel tab data for a given URL and a tab ID"
                         :handler channel/create-channel-tabs-handler}}
      :api/playlist {:get {:summary "returns playlist data for a given URL"
                           :handler playlist/create-playlist-handler}}
      :api/comments {:get {:summary "returns comments data for a given URL"
                           :handler comments/create-comments-handler}}
      :api/swagger-spec {:no-doc true
                         :get    {:swagger {:info     {:title "Tubo API"}
                                            :basePath "/"}
                                  :handler (swagger/create-swagger-handler)}}
      :api/swagger-ui {:no-doc true
                       :get    (swagger-ui/create-swagger-ui-handler)}
      nil)
    (r/expand data opts)))

(defn wrap-cors
  [handler]
  (fn [request]
    ((comp
      #(res/header % "Access-Control-Allow-Methods" "*")
      #(res/header % "Access-Control-Allow-Headers" "Authorization, *")
      #(res/header % "Access-Control-Allow-Origin" "*")
      #(res/header % "Access-Control-Max-Age" "86400")
      handler)
     request)))

(def router
  (ring/router
   routes/routes
   {:expand expand-routes
    :data   {:muuntaja   m/instance
             :middleware [wrap-cors
                          rrc/coerce-exceptions-middleware
                          rrc/coerce-response-middleware
                          muuntaja/format-middleware
                          (exception/create-exception-middleware
                           (merge exception/default-handlers
                                  {::exception/default
                                   (fn [ex _]
                                     {:status 500
                                      :body   {:message (ex-message ex)
                                               :trace   (->> ex
                                                             (.getStackTrace)
                                                             (interpose "\n")
                                                             (apply str))}})}))
                          wrap-params
                          rrc/coerce-request-middleware]}}))

(def app
  (ring/ring-handler
   router
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/redirect-trailing-slash-handler {:method :add})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))
