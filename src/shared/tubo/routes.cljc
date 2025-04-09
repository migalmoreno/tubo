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
   ["/about" :web/about]
   ["/privacy" :web/privacy]
   ["/swagger.json" :api/swagger-spec]
   ["/api-docs/*" :api/swagger-ui]
   ["/api/v1"
    ["/services"
     ["" :api/services]
     ["/:service-id"
      ["/search" :api/search]
      ["/suggestions" :api/suggestions]
      ["/default-kiosk" :api/default-kiosk]
      ["/kiosks"
       ["" :api/all-kiosks]
       ["/:kiosk-id" :api/kiosk]]]]
    ["/streams/:url" :api/stream]
    ["/channels"
     ["/:url"
      ["" :api/channel]
      ["/tabs/:tab-id" :api/channel-tab]]]
    ["/playlists/:url" :api/playlist]
    ["/comments/:url" :api/comments]]])
