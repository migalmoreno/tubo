(ns tubo.downloader
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [tubo.config :as config]
   [tubo.potoken :as potoken])
  (:import
   [okhttp3
    ConnectionSpec
    OkHttpClient$Builder
    Request$Builder
    RequestBody]
   [org.schabi.newpipe.extractor.downloader Downloader Response]
   [org.schabi.newpipe.extractor.exceptions ReCaptchaException]
   org.schabi.newpipe.extractor.localization.Localization
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.ServiceList
   org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
   org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
   org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper))

(defonce user-agent
  "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")

(defn create-request-builder
  [request]
  (let [http-method     (.httpMethod request)
        request-url     (.url request)
        data-to-send    (.dataToSend request)
        request-body    (and data-to-send (RequestBody/create nil data-to-send))
        request-builder (.. (Request$Builder.)
                            (method http-method request-body)
                            (url request-url)
                            (addHeader "User-Agent" user-agent))]
    (doseq [pair (.entrySet (.headers request))]
      (let [header-name       (.getKey pair)
            header-value-list (.getValue pair)]
        (.removeHeader request-builder header-name)
        (doseq [header-value header-value-list]
          (.addHeader request-builder header-name header-value))))
    (.build request-builder)))

(def client
  (.. (OkHttpClient$Builder.)
      (readTimeout 30 java.util.concurrent.TimeUnit/SECONDS)
      (connectionSpecs (list ConnectionSpec/RESTRICTED_TLS))
      (build)))

(defn create-downloader-impl
  []
  (proxy [Downloader] []
    (execute [request]
      (try (let [response (.. client
                              (newCall (create-request-builder request))
                              (execute))
                 body     (.body response)]
             (when (= (.code response) 429)
               (.close response)
               (throw (ReCaptchaException. "reCaptcha Challenge requested"
                                           (.-url request))))
             (Response. (.code response)
                        (.message response)
                        (.. response (headers) (toMultimap))
                        (and body (.string body))
                        (.. response
                            (request)
                            (url)
                            (toString))))
           (catch ReCaptchaException e
             (log/error e))))))

(defmethod ig/init-key ::extractor
  [_ _]
  (NewPipe/init (create-downloader-impl) (Localization. "en" "US"))
  (when (config/get-in [:backend :bg-helper-url])
    (YoutubeStreamExtractor/setPoTokenProvider
     (potoken/create-po-token-provider)))
  (YoutubeParsingHelper/setConsentAccepted
   (config/get-in [:services :youtube :consent-cookie?]))
  (when-let [instance (config/get-in [:services :peertube :default-instance])]
    (.setInstance ServiceList/PeerTube
                  (PeertubeInstance. (:url instance) (:name instance)))))
