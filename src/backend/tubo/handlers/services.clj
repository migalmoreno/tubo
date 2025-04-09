(ns tubo.handlers.services
  (:require
   [clojure.java.data :refer [from-java]]
   [ring.util.response :refer [response]])
  (:import
   org.schabi.newpipe.extractor.NewPipe
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
  (response (map get-service (NewPipe/getServices))))
