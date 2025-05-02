(ns tubo.router
  (:require
   [clojure.tools.logging :as log]
   [muuntaja.core :as m]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.coercion.malli]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.http-response :refer [ok internal-server-error]]
   [tubo.handlers.auth :as auth]
   [tubo.handlers.channel :as channel]
   [tubo.handlers.comments :as comments]
   [tubo.handlers.kiosks :as kiosks]
   [tubo.handlers.playlist :as playlist]
   [tubo.handlers.search :as search]
   [tubo.handlers.services :as services]
   [tubo.handlers.stream :as stream]
   [tubo.middleware :as middleware]
   [tubo.routes :as routes]
   [tubo.schemas :as s]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (case data
      :api/health {:no-doc true
                   :get    (constantly (ok))}
      :api/register {:post {:summary    "registers a user"
                            :parameters {:body {:username s/ValidUsername
                                                :password s/ValidPassword}}
                            :handler    auth/create-register-handler}}
      :api/login {:post {:summary    "logs in a user"
                         :parameters {:body {:username s/ValidUsername
                                             :password s/ValidPassword}}
                         :handler    auth/create-login-handler}}
      :api/logout {:post {:summary    "logs out an authenticated user"
                          :handler    auth/create-logout-handler
                          :middleware [middleware/auth]}}
      :api/delete-user
      {:post {:summary    "deletes an authenticated user"
              :middleware [middleware/auth]
              :parameters {:body {:password s/ValidPassword}}
              :handler    auth/create-delete-user-handler}}
      :api/password-reset
      {:post {:summary    "resets the password for an authenticated user"
              :middleware [middleware/auth]
              :parameters {:body {:current-password s/ValidPassword
                                  :new-password     s/ValidPassword}}
              :handler    auth/create-password-reset-handler}}
      :api/services {:get {:summary "returns all supported services"
                           :handler services/create-services-handler}}
      :api/search {:get {:summary
                         "returns search results for a given service"
                         :parameters {:path  {:service-id int?}
                                      :query {:q string?}}
                         :handler search/create-search-handler}}
      :api/suggestions {:get {:summary
                              "returns search suggestions for a given service"
                              :parameters {:path  {:service-id int?}
                                           :query {:q string?}}
                              :handler search/create-suggestions-handler}}
      :api/instance {:get {:summary
                           "returns the current instance for a given service"
                           :handler services/create-instance-handler}}
      :api/instance-metadata
      {:get {:summary "returns instance metadata for a given service"
             :handler services/create-instance-metadata-handler}}
      :api/change-instance
      {:post {:summary    "changes the instance for a given service"
              :handler    services/create-change-instance-handler
              :parameters {:body s/PeerTubeInstance}}}
      :api/default-kiosk {:get
                          {:summary
                           "returns default kiosk entries for a given service"
                           :parameters {:path {:service-id int?}}
                           :handler kiosks/create-kiosk-handler}}
      :api/all-kiosks {:get {:summary
                             "returns all kiosks supported by a given service"
                             :parameters {:path {:service-id int?}}
                             :handler kiosks/create-kiosks-handler}}
      :api/kiosk {:get
                  {:summary
                   "returns kiosk entries for a given service and a kiosk ID"
                   :parameters {:path {:service-id int? :kiosk-id string?}}
                   :handler kiosks/create-kiosk-handler}}
      :api/stream {:get {:summary    "returns stream data for a given URL"
                         :parameters {:path {:url uri?}}
                         :handler    stream/create-stream-handler}}
      :api/channel {:get {:summary    "returns channel data for a given URL"
                          :parameters {:path {:url uri?}}
                          :handler    channel/create-channel-handler}}
      :api/channel-tab {:get
                        {:summary
                         "returns channel tab data for a given URL and a tab ID"
                         :parameters {:path {:url uri? :tab-id string?}}
                         :handler channel/create-channel-tabs-handler}}
      :api/playlist {:get {:summary    "returns playlist data for a given URL"
                           :parameters {:path {:url uri?}}
                           :handler    playlist/create-playlist-handler}}
      :api/comments {:get {:summary    "returns comments data for a given URL"
                           :parameters {:path {:url uri?}}
                           :handler    comments/create-comments-handler}}
      :api/swagger-spec {:no-doc true
                         :get    {:swagger {:info     {:title "Tubo API"}
                                            :basePath "/"}
                                  :handler (swagger/create-swagger-handler)}}
      :api/swagger-ui {:no-doc true
                       :get    (swagger-ui/create-swagger-ui-handler)}
      (ok))
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
                          (exception/create-exception-middleware
                           (merge exception/default-handlers
                                  {::exception/default
                                   (fn [ex _]
                                     (log/error ex)
                                     (internal-server-error
                                      {:message (ex-message ex)
                                       :trace   (->> ex
                                                     (.getStackTrace)
                                                     (interpose "\n")
                                                     (apply str))}))}))
                          wrap-params
                          coercion/coerce-exceptions-middleware
                          coercion/coerce-request-middleware
                          coercion/coerce-response-middleware]}}))

(defn add-datasource
  [handler datasource]
  (fn [request]
    (handler (assoc request :datasource datasource))))

(defn create-app-handler
  [datasource]
  (ring/ring-handler
   router
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/redirect-trailing-slash-handler {:method :add})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))
   {:middleware [[add-datasource datasource]]}))
