(ns tubo.potoken
  (:require
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [org.httpkit.client :as client]
   [promesa.exec :as px]
   [tubo.config :as config])
  (:import
   [org.schabi.newpipe.extractor.services.youtube PoTokenProvider
    PoTokenResult]))

(defn get-web-visitor-data
  []
  (let [{:keys [body]} @(client/get "https://www.youtube.com")]
    (second (re-find #"visitorData\":\"([\w%-]+)\"" body))))

(defn get-web-client-po-token
  []
  (let [visitor-data   (get-web-visitor-data)
        {:keys [body]} @(client/post (str (config/bg-helper-url (config/config))
                                          "/generate")
                                     {:body (json/write-str
                                             {:visitorData
                                              (get-web-visitor-data)})})]
    (when body
      (PoTokenResult. visitor-data (get (json/read-str body) "poToken") nil))))

(defonce valid-po-tokens (atom ()))

(defn get-po-token
  []
  (let [first-token (first @valid-po-tokens)
        po-token    (if first-token
                      (do
                        (swap! valid-po-tokens #(subvec % 1))
                        first-token)
                      (get-web-client-po-token))]
    (when po-token
      (prn "Trying to get potokens" po-token)
      (px/schedule! 10000 #(swap! valid-po-tokens conj po-token))
      po-token)))

(defn create-po-token-provider
  []
  (reify
   PoTokenProvider
     (getWebClientPoToken [_ _]
       (try
         (get-po-token)
         (catch Exception e
           (log/error e))))
     (getAndroidClientPoToken [_ _])))
