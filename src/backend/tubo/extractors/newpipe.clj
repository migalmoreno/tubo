(ns tubo.extractors.newpipe
  (:require
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [org.httpkit.client :as client]
   [promesa.exec :as px])
  (:import
   [okhttp3 ConnectionSpec OkHttpClient$Builder Request$Builder RequestBody]
   [org.schabi.newpipe.extractor.downloader Downloader Response]
   [org.schabi.newpipe.extractor.exceptions ReCaptchaException]
   [org.schabi.newpipe.extractor.services.youtube PoTokenProvider PoTokenResult]
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

(defmethod ig/init-key ::downloader
  [_ _]
  (NewPipe/init (create-downloader-impl) (Localization. "en" "US")))

(defn get-web-visitor-data
  []
  (let [{:keys [body]} @(client/get "https://www.youtube.com")]
    (second (re-find #"visitorData\":\"([\w%-]+)\"" body))))

(defn get-web-client-po-token
  [config]
  (let [visitor-data   (get-web-visitor-data)
        {:keys [body]} @(client/post (str (:bg-helper/url config)
                                          "/generate")
                                     {:body (json/write-str
                                             {:visitorData visitor-data})})]
    (when body
      (PoTokenResult. visitor-data (get (json/read-str body) "poToken") nil))))

(defonce valid-po-tokens (atom []))

(defn get-po-token
  [config]
  (try
    (let [first-token (first @valid-po-tokens)
          po-token    (if first-token
                        (do
                          (swap! valid-po-tokens #(subvec % 1))
                          first-token)
                        (get-web-client-po-token config))]
      (when po-token
        (px/schedule! 10000 #(swap! valid-po-tokens conj po-token))
        po-token))
    (catch Exception e
      (log/error e))))

(defn create-po-token-provider
  [config]
  (reify
   PoTokenProvider
     (getWebEmbedClientPoToken [_ _] (get-po-token config))
     (getWebClientPoToken [_ _] (get-po-token config))
     (getAndroidClientPoToken [_ _])))

(defmethod ig/init-key ::services
  [_ {:keys [config]}]
  (YoutubeParsingHelper/setConsentAccepted (:youtube/consent-cookie? config))
  (when-let [{:keys [name url]} (:peertube/default-instance config)]
    (.setInstance ServiceList/PeerTube (PeertubeInstance. url name)))
  (when (:bg-helper/url config)
    (YoutubeStreamExtractor/setPoTokenProvider
     (create-po-token-provider config))))
