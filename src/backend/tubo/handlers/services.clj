(ns tubo.handlers.services
  (:require
   [clojure.java.data :refer [from-java]]
   [ring.util.http-response :refer [ok internal-server-error]]
   [ring.util.codec :refer [url-decode]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.ServiceList
   org.schabi.newpipe.extractor.services.peertube.PeertubeInstance
   java.util.Locale))

(defn get-service
  [service]
  {:id                  (.getServiceId service)
   :info                (from-java (.getServiceInfo service))
   :base-url            (.getBaseUrl service)
   :supported-languages (map (fn [lang]
                               {:name (.getDisplayLanguage
                                       (Locale. (.getLanguageCode lang)
                                                (.getCountryCode lang)))
                                :code (.getLocalizationCode lang)})
                             (.getSupportedLocalizations service))
   :supported-countries (map (fn [country]
                               {:name (.getDisplayCountry
                                       (Locale. "" (.toString country)))
                                :code (.toString country)})
                             (.getSupportedCountries service))
   :content-filters     (from-java (.. service
                                       (getSearchQHFactory)
                                       (getAvailableContentFilter)))})

(defn create-services-handler
  [_]
  (ok (map get-service (NewPipe/getServices))))

(defn fetch-instance-metadata
  [url]
  (from-java (doto (PeertubeInstance. (url-decode url))
               (.fetchInstanceMetaData))))

(defn create-instance-handler
  [_]
  (ok (from-java (.getInstance ServiceList/PeerTube))))

(defn create-instance-metadata-handler
  [{{:keys [url]} :path-params}]
  (ok (fetch-instance-metadata url)))

(defn create-change-instance-handler
  [{:keys [body-params]}]
  (try
    (fetch-instance-metadata (:url body-params))
    (.setInstance ServiceList/PeerTube
                  (PeertubeInstance. (:url body-params)
                                     (:name body-params)))
    (ok (str "PeerTube instanced changed to " (:name body-params)))
    (catch Exception _
      (internal-server-error
       "There was a problem changing PeerTube instance"))))
