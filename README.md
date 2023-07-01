

# Tubo

Tubo is a streaming front-end focused on bringing the [NewPipe](https://github.com/TeamNewPipe/NewPipe) experience to the web. It currently supports the same platforms as NewPipe, including YouTube, SoundCloud, and more.  

To retrieve the data, it wraps the excellent [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) library and exposes the extracted data over a REST API that is consumed by a local re-frame SPA.  


## Try It Out

You can try a live demo at <https://tubo.migalmoreno.com>. If you can, please consider self-hosting Tubo and let me know about your instance via the [contribution methods](#org36cfc60). See [installation](#org88ed64e) for ways to set up Tubo in your server.  


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


## Road-map

-   [X] Basic audio player
-   [ ] Track queuing system
-   [ ] Playlists
-   [ ] User settings


## Screenshots

![img](https://files.migalmoreno.com/tubo_kiosk.jpg)  
![img](https://files.migalmoreno.com/tubo_channel.jpg)  
![img](https://files.migalmoreno.com/tubo_stream.jpg)  


## Contributing

Feel free to open an issue with bug reports or feature requests. PRs are more than welcome too.  

