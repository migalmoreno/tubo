(ns tubo.router
  (:require
   [integrant.core :as ig]
   [muuntaja.core :as m]
   [reitit.coercion.malli]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.http-response :refer [ok]]
   [tubo.handlers.auth :as auth]
   [tubo.handlers.auth-playlists :as ap]
   [tubo.handlers.channel :as channel]
   [tubo.handlers.comments :as comments]
   [tubo.handlers.feed :as feed]
   [tubo.handlers.kiosks :as kiosks]
   [tubo.handlers.playlist :as playlist]
   [tubo.handlers.proxy :as proxy]
   [tubo.handlers.search :as search]
   [tubo.handlers.services :as services]
   [tubo.handlers.stream :as stream]
   [tubo.handlers.subscriptions :as sub]
   [tubo.middleware :as middleware]
   [tubo.routes :as routes]
   [tubo.schemas :as s]))

(defn expand-routes
  [data opts]
  (if (keyword? data)
    (case data
      :proxy {:handler proxy/create-proxy-handler}
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
      :api/user-playlists
      {:get    {:summary    "returns all playlists for an authenticated user"
                :handler    ap/create-get-auth-playlists-handler
                :middleware [middleware/auth]}
       :post   {:summary    "creates a new playlist for an authenticated user"
                :handler    ap/create-post-auth-playlists-handler
                :middleware [middleware/auth]
                :parameters {:body s/UserPlaylist}}
       :delete {:summary    "deletes all playlists for an authenticated user"
                :handler    ap/create-delete-auth-playlists-handler
                :middleware [middleware/auth]}}
      :api/user-subscriptions
      {:get    {:summary "returns all subscriptions for an authenticated user"
                :handler sub/create-get-subscriptions-handler
                :middleware [middleware/auth]}
       :post   {:summary    "adds a new subscription for an authenticated user"
                :handler    sub/create-post-subscriptions-handler
                :middleware [middleware/auth]
                :parameters {:body s/SubscriptionChannel}}
       :delete {:summary "deletes all subscriptions for an authenticated user"
                :handler sub/create-delete-subscriptions-handler
                :middleware [middleware/auth]}}
      :api/user-subscription
      {:delete {:summary    "deletes a subscription for an authenticated user"
                :parameters {:path {:url string?}}
                :handler    sub/create-delete-subscription-handler
                :middleware [middleware/auth]}}
      :api/feed {:get     {:summary
                           "returns latest streams for a list of channel URLs"}
                 :handler feed/create-get-feed-handler}
      :api/user-feed
      {:get
       {:summary
        "returns latest streams for an authenticated user's subscriptions"
        :handler feed/create-get-user-feed-handler
        :middleware [middleware/auth]}}
      :api/add-user-playlist-streams
      {:post {:summary    "adds new playlist streams for a given user playlist"
              :handler    ap/create-post-auth-playlist-add-streams-handler
              :middleware [middleware/auth]
              :parameters {:body [:vector s/UserPlaylistStream]}}}
      :api/delete-user-playlist-stream
      {:post {:summary    "deletes playlist stream for a given user playlist"
              :handler    ap/create-post-auth-playlist-delete-stream-handler
              :middleware [middleware/auth]
              :parameters {:body s/UserPlaylistStream}}}
      :api/user-playlist
      {:get    {:summary    "returns a user playlist for an authenticated user"
                :parameters {:path {:id string?}}
                :handler    ap/create-get-auth-playlist-handler
                :middleware [middleware/auth]}
       :put    {:summary    "updates a user playlist for an authenticated user"
                :parameters {:path {:id string?}
                             :body s/UserPlaylist}
                :handler    ap/create-update-auth-playlist-handler
                :middleware [middleware/auth]}
       :delete {:summary    "deletes a user playlist for an authenticated user"
                :parameters {:path {:id string?}}
                :handler    ap/create-delete-auth-playlist-handler
                :middleware [middleware/auth]}}
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
                         :handler channel/create-channel-tab-handler}}
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
                          middleware/exception-middleware
                          wrap-params
                          coercion/coerce-exceptions-middleware
                          coercion/coerce-request-middleware
                          coercion/coerce-response-middleware]}}))

(defmethod ig/init-key ::handler
  [_ {:keys [datasource]}]
  #(ring/ring-handler
    router
    (ring/routes
     (ring/create-resource-handler {:path "/"})
     (ring/redirect-trailing-slash-handler {:method :add})
     (ring/create-default-handler
      {:not-found (constantly {:status 404 :body "Not found"})}))
    {:middleware [[middleware/wrap-datasource datasource]]}))
