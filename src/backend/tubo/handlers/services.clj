(ns tubo.handlers.services
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.java.data :as j]
   [ring.util.codec :refer [url-decode]]
   [ring.util.http-response :refer [internal-server-error ok]])
  (:import
   java.util.Locale
   org.schabi.newpipe.extractor.NewPipe
   org.schabi.newpipe.extractor.ServiceList
   org.schabi.newpipe.extractor.services.peertube.PeertubeInstance))

(defn create-services-handler
  [_]
  (when-let [services (NewPipe/getServices)]
    (->> (j/from-java-deep services {:omit #{:suggestionExtractor :kioskList}})
         (cske/transform-keys csk/->kebab-case-keyword)
         (map
          (fn [service]
            (-> service
                (update :supported-localizations
                        (fn [locales]
                          (map #(assoc %
                                       :name
                                       (.getDisplayLanguage
                                        (Locale. (:language-code %)
                                                 (:country-code %))))
                               locales)))
                (update :supported-countries
                        (fn [countries]
                          (map #(assoc %
                                       :name
                                       (.getDisplayCountry
                                        (Locale. "" (:country-code %))))
                               countries))))))
         ok)))

(defn create-instance-handler
  [_]
  (ok (j/from-java-shallow (.getInstance ServiceList/PeerTube) {})))

(defn fetch-instance-metadata
  [url]
  (j/from-java-shallow (doto (PeertubeInstance. (url-decode url))
                         (.fetchInstanceMetaData))
                       {}))

(defn create-instance-metadata-handler
  [{{:keys [url]} :path-params}]
  (ok (fetch-instance-metadata url)))

(defn create-change-instance-handler
  [{{:keys [url name]} :body-params}]
  (try
    (fetch-instance-metadata url)
    (.setInstance ServiceList/PeerTube (PeertubeInstance. url name))
    (ok (str "PeerTube instance changed to " name))
    (catch Exception _
      (internal-server-error
       "There was a problem changing PeerTube instance"))))
