

# Tubo

Tubo is a streaming front-end focused on bringing the [NewPipe](https://github.com/TeamNewPipe/NewPipe) experience to the web. It aims at providing a clean and simple user interface to consume media from your favorite streaming platforms. It currently supports the same services as NewPipe, including YouTube, SoundCloud, Bandcamp, and more.

To retrieve the data, it wraps the excellent [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) library and it exposes the extracted data over a REST API that is consumed by a local re-frame SPA.


## Screenshots

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left"><img src="https://files.migalmoreno.com/tubo_kiosk_light.jpg" alt="tubo_kiosk_light.jpg" /></th>
<th scope="col" class="org-left"><img src="https://files.migalmoreno.com/tubo_channel_light.jpg" alt="tubo_channel_light.jpg" /></th>
<th scope="col" class="org-left"><img src="https://files.migalmoreno.com/tubo_stream_playing_light.jpg" alt="tubo_stream_playing_light.jpg" /></th>
<th scope="col" class="org-left"><img src="https://files.migalmoreno.com/tubo_queue_light.jpg" alt="tubo_queue_light.jpg" /></th>
<th scope="col" class="org-left"><img src="https://files.migalmoreno.com/tubo_settings_light.jpg" alt="tubo_settings_light.jpg" /></th>
</tr>
</thead>

<tbody>
<tr>
<td class="org-left"><img src="https://files.migalmoreno.com/tubo_kiosk_dark.jpg" alt="tubo_kiosk_dark.jpg" /></td>
<td class="org-left"><img src="https://files.migalmoreno.com/tubo_channel_dark.jpg" alt="tubo_channel_dark.jpg" /></td>
<td class="org-left"><img src="https://files.migalmoreno.com/tubo_stream_dark.jpg" alt="tubo_stream_dark.jpg" /></td>
<td class="org-left"><img src="https://files.migalmoreno.com/tubo_queue_dark.jpg" alt="tubo_queue_dark.jpg" /></td>
<td class="org-left"><img src="https://files.migalmoreno.com/tubo_bookmarks_dark.jpg" alt="tubo_bookmarks_dark.jpg" /></td>
</tr>
</tbody>
</table>


## Features

-   [X] No ads
-   [X] Audio player
-   [X] Media queue
-   [X] Playlists
-   [X] Settings
-   [ ] Subscriptions
-   [ ] User login


## Instances

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left">URL</th>
<th scope="col" class="org-left">Country</th>
</tr>
</thead>

<tbody>
<tr>
<td class="org-left"><a href="https://tubo.migalmoreno.com">https://tubo.migalmoreno.com</a> (Official)</td>
<td class="org-left">🇪🇸</td>
</tr>

<tr>
<td class="org-left"><a href="https://tubo.reallyaweso.me">https://tubo.reallyaweso.me</a></td>
<td class="org-left">🇩🇪</td>

<tr>
<td class="org-left"><a href="https://tubo.reallyaweso.me">https://tubo.reallyaweso.me</a></td>
<td class="org-left">🇩🇪</td>
</tr>


<tr>
<td class="org-left"><a href="https://tubo.reallyaweso.me">https://tubo.reallyaweso.me</a></td>
<td class="org-left">🇩🇪</td>
</tr>
</tbody>
</table>

If you consider self-hosting Tubo let me know about your instance via the [contribution methods](#org26cd4a5). See [installation](#org5693e96) for ways to set up Tubo in your server.


## Installation


### Packaging

-   Uberjar

    To bundle the whole project into a self-contained uber-jar you need to follow these build steps:

        npm i
        npm run build
        clojure -T:frontend:build uberjar

    After the last command is completed, you'll get a path to the uber-jar, which you can run like this:

        java -jar target/tubo-<VERSION>.jar

-   Docker

    Alternatively, you can use Docker to set up Tubo. Simply invoke this:

        docker-compose up -d

-   Manual

    You can also set up Tubo manually via the [GNU Guix](https://guix.gnu.org/) package manager. First, download the necessary tooling:

        cd /path/to/tubo
        guix shell

    Then, compile the downloader ahead-of-time:

        clojure -M -e "(compile 'tubo.downloader-impl)"

    Fetch the front-end dependencies and build the front-end assets.

        npm i
        npm run build

    Finally, compile the front-end.

        clojure -M:frontend release tubo

    You can now start a local server that listens on port 3000 by running this:

        clojure -M:run

    Access the front-end in your browser at `http://localhost:3000`.


### Reverse Proxy

If you want to self-host Tubo and make it publicly accessible you'll need to set up a reverse proxy.

-   Nginx

        server {
            listen 443 ssl http2;
            server_name tubo.<YOUR_DOMAIN>;
            ssl_certificate /etc/letsencrypt/live/tubo.<YOUR_DOMAIN>/fullchain.pem;
            ssl_certificate_key /etc/letsencrypt/live/tubo.<YOUR_DOMAIN>/privkey.pem;

            location / {
                proxy_pass http://localhost:3000;
                proxy_set_header X-Forwarded-For $remote_addr;
                proxy_set_header HOST $http_host;
            }
        }


## Browser Extension Support


### [Redirector](https://github.com/einaregilsson/Redirector)

You can manually add any redirect rule based on regex patterns with this extension. Below are some sample configurations to redirect links from supported services to Tubo so you can get a basic idea of how to write manual Redirector rules. Note the `serviceId` of each service is: YouTube (0), SoundCloud(1), media.ccc.de(2), PeerTube(3), and Bandcamp(4). Replace <https://tubo.migalmoreno.com/> in the redirect rule to the instance of your choice.

    Description: YouTube video to Tubo stream
    Example URL: https://www.youtube.com/watch?v=YE7VzlLtp-4
    Include pattern: ^((?:https?://)(?:www.)?youtube.com/(watch\?v.*|shorts/.*))
    Redirect to: https://tubo.migalmoreno.com/stream?url=$1
    Example result:
    https://tubo.migalmoreno.com/stream?url=https://www.youtube.com/watch?v=YE7VzlLtp-4
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: SoundCloud stream to Tubo stream
    Example URL: https://soundcloud.com/unfa/stop-the-panic
    Include pattern: ^((?:https?://)(?:www.)?soundcloud.com/.*/.*)
    Redirect to: https://tubo.migalmoreno.com/stream?url=$1
    Example result:
    https://tubo.migalmoreno.com/stream?url=https://soundcloud.com/unfa/stop-the-panic
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: Bandcamp album to Tubo playlist
    Example URL: https://unfa.bandcamp.com/album/suppressed
    Include pattern: ^((?:https?://)(.*\.)?bandcamp.com/album/.*)
    Redirect to: https://tubo.migalmoreno.com/playlist?url=$1
    Example result: https://tubo.migalmoreno.com/playlist?url=https://unfa.bandcamp.com/album/suppressed
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: PeerTube (Framatube) channel to Tubo channel
    Example URL: https://framatube.org/accounts/framasoft@framatube.org
    Include pattern: ^((?:https?://)(?:www.)?framatube.org/accounts/.*)
    Redirect to: https://tubo.migalmoreno.com/channel?url=$1
    Example result:
    https://tubo.migalmoreno.com/channel?url=https://framatube.org/accounts/framasoft@framatube.org
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: media.ccc.de search query to Tubo search query
    Example URL: https://media.ccc.de/search/?q=37c3
    Include pattern: ^(?:https?://)media.ccc.de/search/\?q=(.*)
    Redirect to: https://tubo.migalmoreno.com/search?query=$1&serviceId=2
    Example result: https://tubo.migalmoreno.com/search?query=37c3&serviceId=2
    Pattern type: Regular Expression
    Apply to: Main window (address bar)


### [LibRedirect](https://github.com/libredirect/browser_extension)

Redirects many popular services to their alternative front-ends. Has a ton of features and an active community. Tubo is supported by default for YouTube and SoundCloud, so no need to do anything for these. The rest of services are pending as per [#884](https://github.com/libredirect/browser_extension/issues/884).


### [Privacy Redirector](https://github.com/dybdeskarphet/privacy-redirector)

A userscript that redirects popular social media platforms to their privacy respecting front-ends.


### [nx-router](https://github.com/migalmoreno/nx-router)

Similar to Redirector but for the [Nyxt](https://nyxt.atlas.engineer/) browser, you can manually add any redirect rule based on regex patterns with this extension, which allows you to define all redirection rules in a single "router". A sample configuration for YouTube would look like this:

    (make-instance 'router:redirector
                   :name 'youtube-to-tubo
                   :route (match-domain "youtube.com")
                   :redirect
                   '(("https://tubo.migalmoreno.com/stream?url=\\&" . (".*/watch\\?v.*" ".*/shorts/.*"))
                     ("https://tubo.migalmoreno.com/playlist?list=\\&" . ".*/playlist/.*")
                     ("https://tubo.migalmoreno.com/channel?url=\\&" . ".*/channel/.*")
                     ("https://tubo.migalmoreno.com/search?q=\\1&serviceId=0" . ".*/search\\?q=(.*)")))


## Contributing

Feel free to open an issue with bug reports or feature requests. PRs are more than welcome too.
