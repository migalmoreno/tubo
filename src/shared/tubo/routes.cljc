(ns tubo.routes)

(def routes
  [["/" :web/homepage]
   ["/search" :web/search]
   ["/stream" :web/stream]
   ["/channel" :web/channel]
   ["/playlist" :web/playlist]
   ["/kiosk" :web/kiosk]
   ["/settings" :web/settings]
   ["/bookmark" :web/bookmark]
   ["/bookmarks" :web/bookmarks]
   ["/subscriptions" :web/subscriptions]
   ["/feed" :web/feed]
   ["/about" :web/about]
   ["/privacy" :web/privacy]
   ["/swagger.json" :api/swagger-spec]
   ["/api-docs/*" :api/swagger-ui]
   ["/login" :web/login]
   ["/register" :web/register]
   ["/proxy/:url" :proxy]
   ["/api/v1"
    ["/health" :api/health]
    ["/register" :api/register]
    ["/login" :api/login]
    ["/logout" :api/logout]
    ["/password-reset" :api/password-reset]
    ["/delete-user" :api/delete-user]
    ["/feed" :api/feed]
    ["/user"
     ["/playlists"
      ["" :api/user-playlists]
      ["/:id"
       ["" :api/user-playlist]
       ["/add-streams" :api/add-user-playlist-streams]
       ["/delete-stream" :api/delete-user-playlist-stream]]]
     ["/subscriptions"
      ["" :api/user-subscriptions]
      ["/:url" :api/user-subscription]]
     ["/feed" :api/user-feed]]
    ["/services"
     ["" :api/services]
     ["/:service-id"
      ["/search" :api/search]
      ["/suggestions" :api/suggestions]
      ["/default-kiosk" :api/default-kiosk]
      ["/kiosks"
       ["" :api/all-kiosks]
       ["/:kiosk-id" :api/kiosk]]]
     ["/3"
      ["/instance" :api/instance]
      ["/instance-metadata/:url" :api/instance-metadata]
      ["/change-instance" :api/change-instance]]]
    ["/streams/:url" :api/stream]
    ["/channels"
     ["/:url"
      ["" :api/channel]
      ["/tabs/:tab-id" :api/channel-tab]]]
    ["/playlists/:url" :api/playlist]
    ["/comments/:url" :api/comments]]])
