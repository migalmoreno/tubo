(ns tubo.downloader
  (:require
   [clojure.tools.logging :as log]
   [org.httpkit.client :as http]
   [clojure.java.data :as j]
   [clojure.string :as str])
  (:import
   [org.schabi.newpipe.extractor.downloader Downloader Response]
   [org.schabi.newpipe.extractor.exceptions ReCaptchaException]))

(defonce user-agent
  "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")

(defn create-downloader-impl
  []
  (proxy [Downloader] []
    (execute [request]
      (try
        (let [req-headers (reduce-kv #(assoc %1 %2 (str/join ", " %3))
                                     {}
                                     (j/from-java-deep (.headers request)
                                                       {:exceptions :omit}))
              {:keys [status headers body opts]}
              @(http/request
                {:url        (.url request)
                 :method     (keyword (str/lower-case (.httpMethod request)))
                 :body       (.dataToSend request)
                 :as         :text
                 :headers    req-headers
                 :timeout    30000
                 :user-agent user-agent})
              res-headers (reduce-kv #(assoc %1 (name %2) [%3]) {} headers)]
          (when (= status 429)
            (throw (ReCaptchaException. "reCaptcha Challenge requested"
                                        (.-url request))))
          (Response. status nil res-headers body (:url opts)))
        (catch ReCaptchaException e
          (log/error e))))))
