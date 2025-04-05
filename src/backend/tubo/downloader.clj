(ns tubo.downloader
  (:require
   [clojure.tools.logging :as log])
  (:import
   [org.schabi.newpipe.extractor.downloader Downloader Response]
   [org.schabi.newpipe.extractor.exceptions ReCaptchaException]
   [okhttp3 Request$Builder OkHttpClient$Builder RequestBody]))

(defonce user-agent
  "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")

(defn create-request-builder
  [request]
  (let [http-method     (.httpMethod request)
        url             (.url request)
        data-to-send    (.dataToSend request)
        request-body    (and data-to-send
                             (RequestBody/create nil data-to-send))
        request-builder (.. (Request$Builder.)
                            (method http-method request-body)
                            (url url)
                            (addHeader "User-Agent" user-agent))]
    (doseq [pair (.entrySet (.headers request))]
      (let [header-name       (.getKey pair)
            header-value-list (.getValue pair)]
        (if (> (.size header-value-list) 1)
          (do
            (.removeHeader request-builder header-name)
            (doseq [header-value header-value-list]
              (.addHeader request-builder header-name header-value)))
          (when (= (.size header-value-list) 1)
            (.header request-builder
                     header-name
                     (.get header-value-list 0))))))
    request-builder))

(defn create-downloader-impl
  []
  (let [client (.. (OkHttpClient$Builder.)
                   (readTimeout 30 java.util.concurrent.TimeUnit/SECONDS)
                   (build))]
    (proxy [Downloader] []
      (execute [request]
        (try (let [response (.. client
                                (newCall (.build (create-request-builder
                                                  request)))
                                (execute))
                   body     (.body response)]
               (when (= (.code response) 429)
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
               (log/error e)))))))
