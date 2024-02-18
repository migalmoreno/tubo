(ns tubo.routes
  (:require
   [malli.experimental.lite :as l]
   [reitit.ring :as ring]
   [reitit.coercion :as coercion]
   [reitit.ring.coercion :as rrc]
   [reitit.coercion.malli]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.cors :refer [wrap-cors]]
   [tubo.handler :as handler]))

(def router
  (ring/router
   [["/" handler/index]
    ["/search" handler/index]
    ["/stream" handler/index]
    ["/channel" handler/index]
    ["/playlist" handler/index]
    ["/kiosk" handler/index]
    ["/settings" handler/index]
    ["/bookmark" handler/index]
    ["/bookmarks" handler/index]
    ["/api"
     ["/services"
      ["" {:get handler/services}]
      ["/:service-id/search"
       {:get {:coercion reitit.coercion.malli/coercion
              :parameters {:path {:service-id int?}
                           :query {:q string?}}
              :handler handler/search}}]
      ["/:service-id"
       ["/default-kiosk" {:get {:coercion reitit.coercion.malli/coercion
                                :parameters {:path {:service-id int?}}
                                :handler handler/kiosk}}]
       ["/kiosks"
        ["" {:get {:coercion reitit.coercion.malli/coercion
                   :parameters {:path {:service-id int?}}
                   :handler handler/kiosks}}]
        ["/:kiosk-id" {:get {:coercion reitit.coercion.malli/coercion
                             :parameters {:path {:service-id int? :kiosk-id string?}}
                             :handler handler/kiosk}}]]]]
     ["/streams/:url" {:get handler/stream}]
     ["/channels/:url" {:get handler/channel}]
     ["/playlists/:url" {:get handler/playlist}]
     ["/comments/:url" {:get handler/comments}]]]
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
