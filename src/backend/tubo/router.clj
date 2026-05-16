(ns tubo.router
  (:require
   [integrant.core :as ig]
   [muuntaja.core :as m]
   [reitit.coercion.malli]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers :as handlers]
   [tubo.auth.handlers :as auth]
   [tubo.extractors.handlers :as extractors]
   [tubo.playlists.handlers :as playlists]
   [tubo.subscriptions.handlers :as subscriptions]
   [tubo.middleware :as middleware]
   [tubo.routes :as routes]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (-> (merge auth/routes
               extractors/routes
               handlers/routes
               playlists/routes
               subscriptions/routes)
        (get data (ok)))
    (r/expand data opts)))

(def router
  (ring/router
   routes/routes
   {:expand expand-routes
    :data   {:coercion   reitit.coercion.malli/coercion
             :muuntaja   m/instance
             :middleware [middleware/wrap-cors
                          middleware/wrap-token-auth
                          muuntaja/format-middleware
                          middleware/exception-middleware
                          wrap-params
                          coercion/coerce-exceptions-middleware
                          coercion/coerce-request-middleware
                          coercion/coerce-response-middleware]}}))

(defmethod ig/init-key ::handler
  [_ {:keys [datasource config]}]
  #(ring/ring-handler
    router
    (ring/routes
     (ring/create-resource-handler {:path "/"})
     (ring/redirect-trailing-slash-handler {:method :add})
     (ring/create-default-handler
      {:not-found (constantly {:status 404 :body "Not found"})}))
    {:middleware [[middleware/wrap-config config]
                  [middleware/wrap-datasource datasource]]}))
