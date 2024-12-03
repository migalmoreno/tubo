(ns tubo.downloader-impl
  (:import
   [org.schabi.newpipe.extractor.downloader Response]
   [okhttp3 Request$Builder OkHttpClient$Builder RequestBody]))

(gen-class
 :name         tubo.DownloaderImpl
 :constructors {[okhttp3.OkHttpClient$Builder] []}
 :extends      org.schabi.newpipe.extractor.downloader.Downloader
 :init         downloader-impl)

(gen-class
 :name         tubo.DownloaderImpl
 :constructors {[okhttp3.OkHttpClient$Builder] []}
 :extends      org.schabi.newpipe.extractor.downloader.Downloader
 :prefix       "-"
 :main         false
 :state        state
 :init         downloader-impl
 :methods      [#^{:static true} [init [] tubo.DownloaderImpl]
                #^{:static true} [getInstance [] tubo.DownloaderImpl]])

(def user-agent
  "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")
(def instance (atom nil))

(defn -downloader-impl
  [builder]
  [[]
   (atom {:client
          (.. builder
              (readTimeout 30 java.util.concurrent.TimeUnit/SECONDS)
              (build))})])

(defn -init
  ([]
   (-init (OkHttpClient$Builder.)))
  ([builder]
   (reset! instance (tubo.DownloaderImpl. builder))))

(defn -getInstance
  []
  (or @instance (-init)))

(defn -execute
  [this request]
  (let [http-method     (.httpMethod request)
        url             (.url request)
        headers         (.headers request)
        data-to-send    (.dataToSend request)
        request-body    (and data-to-send (RequestBody/create nil data-to-send))
        request-builder (.. (Request$Builder.)
                            (method http-method request-body)
                            (url url)
                            (addHeader "User-Agent" user-agent))]
    (doseq [pair (.entrySet headers)]
      (let [header-name       (.getKey pair)
            header-value-list (.getValue pair)]
        (if (> (.size header-value-list) 1)
          (do
            (.removeHeader request-builder header-name)
            (doseq [header-value header-value-list]
              (.addHeader request-builder header-name header-value)))
          (when (= (.size header-value-list) 1)
            (.header request-builder header-name (.get header-value-list 0))))))
    (let [response                (.. (@(.state this) :client)
                                      (newCall (.build request-builder))
                                      (execute))
          body                    (.body response)
          response-body-to-return (and body (.string body))
          latest-url              (.. response (request) (url) (toString))]
      (when (= (.code response) 429)
        (.close response))
      (Response. (.code response)
                 (.message response)
                 (.. response (headers) (toMultimap))
                 response-body-to-return
                 latest-url))))

(comment
  (compile 'tubo.downloader-impl))
