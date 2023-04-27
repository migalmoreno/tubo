

# Tubo

Tubo is an alternative web front-end to various streaming sites. It aims to free users from the world of ad-ridden streaming sites by providing a distraction-free interface to consume content from. It currently supports the following platforms:  

-   YouTube
-   SoundCloud
-   media.ccc.de
-   PeerTube
-   Bandcamp

To retrieve the data, it leverages the excellent [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) library that powers the popular [NewPipe](https://github.com/TeamNewPipe/NewPipe) Android app. Tubo exposes the extracted data over a REST API that is consumed by a local re-frame SPA.  

The ultimate goal behind Tubo is to replicate the Newpipe experience on the web, so that it's accessible to those that don't use an Android device.  


## Installation

The easiest way to set up Tubo's dependencies is via the [GNU Guix](https://guix.gnu.org/) package manager. Simply invoke what follows:  

    cd /path/to/tubo
    guix shell

To run the application, first compile the downloader ahead-of-time.  

    clojure -M -e "(compile 'tubo.downloader-impl)"

Fetch the front-end dependencies and build the front-end assets.  

    npm i
    npm run build

Then, compile the front-end.  

    clojure -M:frontend compile tubo

You can now start a local server that listens on port 3000 by running the following:  

    clojure -M:run

Access the front-end in your browser at `http://localhost:3000`.  


## Road-map

-   [X] Basic audio player
-   [ ] Track queuing system
-   [ ] Playlists
-   [ ] User settings


## Screenshots

![img](https://files.mianmoreno.com/tubo_kiosk.jpg)  
![img](https://files.mianmoreno.com/tubo_channel.jpg)  
![img](https://files.mianmoreno.com/tubo_stream.jpg)  


## Contributing

You can use the project's [mailing list](https://lists.sr.ht/~mmoreno/tubo) to send feedback, patches or open discussions. Bugs should be reported on the project's [bug-tracker](https://todo.sr.ht/~mmoreno/tubo).  

