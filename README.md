

# Tubo

Tubo is a streaming front-end focused on bringing the [NewPipe](https://newpipe.net/) experience to the web. It aims at providing a clean and simple user interface to consume media from your favorite streaming platforms. It currently supports the same services as NewPipe, including YouTube, SoundCloud, Bandcamp, and more.  


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

    You can use Docker Compose to set up Tubo.  
    
        docker-compose up -d

-   Manual

    You set up Tubo manually via the [GNU Guix](https://guix.gnu.org/) package manager. First, download the necessary tooling:  
    
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
            server_name <TUBO_HOST>;
            ssl_certificate /etc/letsencrypt/live/<TUBO_HOST>/fullchain.pem;
            ssl_certificate_key /etc/letsencrypt/live/<TUBO_HOST>/privkey.pem;
        
            location / {
                proxy_pass http://localhost:3000;
                proxy_set_header X-Forwarded-For $remote_addr;
                proxy_set_header HOST $http_host;
            }
        }


## Browser Extension Support


### [Redirector](https://einaregilsson.com/redirector/)

You can manually add any redirect rule based on regex patterns with this extension. Below are some sample configurations to redirect links from supported services to Tubo so you get a basic idea of how to write manual Redirector rules. Note the `serviceId` of each service is: YouTube (0), SoundCloud(1), media.ccc.de(2), PeerTube(3), and Bandcamp(4).  

    Description: YouTube video to Tubo stream
    Example URL: https://www.youtube.com/watch?v=YE7VzlLtp-4
    Include pattern: ^((?:https?://)(?:www.)?youtube.com/(watch\?v.*|shorts/.*))
    Redirect to: https://<TUBO_HOST>/stream?url=$1
    Example result:
    https://<TUBO_HOST>/stream?url=https://www.youtube.com/watch?v=YE7VzlLtp-4
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: SoundCloud stream to Tubo stream
    Example URL: https://soundcloud.com/unfa/stop-the-panic
    Include pattern: ^((?:https?://)(?:www.)?soundcloud.com/.*/.*)
    Redirect to: https://<TUBO_HOST>/stream?url=$1
    Example result:
    https://<TUBO_HOST>/stream?url=https://soundcloud.com/unfa/stop-the-panic
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: Bandcamp album to Tubo playlist
    Example URL: https://unfa.bandcamp.com/album/suppressed
    Include pattern: ^((?:https?://)(.*\.)?bandcamp.com/album/.*)
    Redirect to: https://<TUBO_HOST>/playlist?url=$1
    Example result: https://<TUBO_HOST>/playlist?url=https://unfa.bandcamp.com/album/suppressed
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: PeerTube (Framatube) channel to Tubo channel
    Example URL: https://framatube.org/accounts/framasoft@framatube.org
    Include pattern: ^((?:https?://)(?:www.)?framatube.org/accounts/.*)
    Redirect to: https://<TUBO_HOST>/channel?url=$1
    Example result:
    https://<TUBO_HOST>/channel?url=https://framatube.org/accounts/framasoft@framatube.org
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

    Description: media.ccc.de search query to Tubo search query
    Example URL: https://media.ccc.de/search/?q=37c3
    Include pattern: ^(?:https?://)media.ccc.de/search/\?q=(.*)
    Redirect to: https://<TUBO_HOST>/search?query=$1&serviceId=2
    Example result: https://<TUBO_HOST>/search?query=37c3&serviceId=2
    Pattern type: Regular Expression
    Apply to: Main window (address bar)

