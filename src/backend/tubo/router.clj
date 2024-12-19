(ns tubo.router
  (:require
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.coercion.malli]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-response]]
   [tubo.handler :as handler]
   [tubo.routes :as routes]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (case data
      :api/services {:get {:summary "returns all supported services"
                           :handler handler/services}}
      :api/search {:get {:summary
                         "returns search results for a given service"
                         :coercion reitit.coercion.malli/coercion
                         :parameters {:path  {:service-id int?}
                                      :query {:q string?}}
                         :handler handler/search}}
      :api/default-kiosk {:get
                          {:summary
                           "returns default kiosk entries for a given service"
                           :coercion reitit.coercion.malli/coercion
                           :parameters {:path {:service-id int?}}
                           :handler handler/kiosk}}
      :api/all-kiosks {:get {:summary
                             "returns all kiosks supported by a given service"
                             :coercion reitit.coercion.malli/coercion
                             :parameters {:path {:service-id int?}}
                             :handler handler/kiosks}}
      :api/kiosk {:get
                  {:summary
                   "returns kiosk entries for a given service and a kiosk ID"
                   :coercion reitit.coercion.malli/coercion
                   :parameters {:path {:service-id int? :kiosk-id string?}}
                   :handler handler/kiosk}}
      :api/stream {:get {:summary "returns stream data for a given URL"
                         :handler handler/stream}}
      :api/channel {:get {:summary "returns channel data for a given URL"
                          :handler handler/channel}}
      :api/channel-tab {:get
                        {:summary
                         "returns channel tab data for a given URL and a tab ID"
                         :handler handler/channel-tabs}}
      :api/playlist {:get {:summary "returns playlist data for a given URL"
                           :handler handler/playlist}}
      :api/comments {:get {:summary "returns comments data for a given URL"
                           :handler handler/comments}}
      :api/swagger-spec {:no-doc true
                         :get    {:swagger {:info     {:title "Tubo API"}
                                            :basePath "/"}
                                  :handler (swagger/create-swagger-handler)}}
      :api/swagger-ui {:no-doc true
                       :get    (swagger-ui/create-swagger-ui-handler)}
      {:no-doc  true
       :handler handler/index})
    (r/expand data opts)))

(def router
  (ring/router
   routes/routes
   {:expand expand-routes
    :data   {:middleware [rrc/coerce-request-middleware
                          rrc/coerce-response-middleware
                          rrc/coerce-exceptions-middleware]}}))

(def app
  (ring/ring-handler
   router
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/redirect-trailing-slash-handler {:method :add})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))
   {:middleware [wrap-params
                 [wrap-json-response {:pretty true}]
                 wrap-reload]}))
