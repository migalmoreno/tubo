(ns tubo.bg-helper
  (:require
   [tubo.config :as config]
   ["bgutils-js" :refer [BG]]
   ["http" :as http]
   ["jsdom" :refer [JSDOM]]
   [promesa.core :as p]))

(defonce dom (JSDOM.))
(set! (.-window js/globalThis) (.-window dom))
(set! (.-document js/globalThis) (.. dom -window -document))

(defn get-po-token
  [visitor-data]
  (-> (p/let
        [bg-config #js {"fetch"      (fn [input init] (js/fetch input init))
                        "identifier" visitor-data
                        "requestKey" "O43z0dpjhgX20SCx4KAo"
                        "globalObj"  js/globalThis}
         ^js bg-challenge (.. BG -Challenge (create bg-config))
         interpreter
         (.. bg-challenge
             -interpreterJavascript
             -privateDoNotAccessOrElseSafeScriptWrappedValue)]
        (if interpreter
          ((js/Function. interpreter))
          (throw (js/Error "Could not load VM")))
        (.. BG
            -PoToken
            (generate #js {"program"    (.-program bg-challenge)
                           "globalName" (.-globalName bg-challenge)
                           "bgConfig"   bg-config})))
      (p/then (fn [^js res]
                (p/let [placeholder-po-token
                        (.. BG -PoToken (generatePlaceholder visitor-data))]
                  #js {"visitorData"        visitor-data
                       "placeholderPotoken" placeholder-po-token
                       "poToken"            (.-poToken res)
                       "integrityTokenData" (.-integrityTokenData res)})))
      (p/catch #(js/console.error %))))

(defonce server-ref (atom nil))

(defn handle-create-po-token
  [^js req ^js res]
  (if (and (= (.-url req) "/generate") (= (.-method req) "POST"))
    (let [body (atom [])]
      (.on req "data" #(swap! body conj %))
      (.on req
           "end"
           (fn []
             (let [parsed-body (-> @body
                                   clj->js
                                   (.join "")
                                   js/JSON.parse)]
               (.setHeader res "Content-Type" "application/json")
               (->
                 (get-po-token (.-visitorData parsed-body))
                 (p/then #(.end res (js/JSON.stringify %))))))))
    (.. res (writeHead 404) (end))))

(defn main
  [& _]
  (let [server (http/createServer #(handle-create-po-token %1 %2))
        port   (config/bg-helper-port (config/config))]
    (.listen server
             port
             #(js/console.log (str "BG helper server running on port " port)))
    (reset! server-ref server)))

(defn start [] (main))

(defn stop
  [done]
  (when-some [srv @server-ref]
    (.close srv
            (fn []
              (js/console.log "stop completed")
              (done)))))
