(ns tubo.downloader
  (:require
   [clojure.tools.logging :as log])
  (:import
   [org.schabi.newpipe.extractor.downloader Downloader Response]
   [org.schabi.newpipe.extractor.exceptions ReCaptchaException]
   [okhttp3 Request$Builder OkHttpClient$Builder RequestBody ConnectionSpec]))

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
