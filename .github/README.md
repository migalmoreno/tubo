

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
</tbody>
</table>

If you consider self-hosting Tubo let me know about your instance via the [contribution methods](#org190dbd6). See [installation](#orgc634111) for ways to set up Tubo in your server.  


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


## Contributing

Feel free to open an issue with bug reports or feature requests. PRs are more than welcome too.  

