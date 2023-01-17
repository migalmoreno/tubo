(ns tubo.api
  (:require
   [ajax.core :as ajax]))

(defn get-request
  ([uri on-success on-failure]
   (get-request uri on-success on-failure {}))
  ([uri on-success on-failure params]
   {:http-xhrio {:method :get
                 :uri uri
                 :params params
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success on-success
                 :on-failure on-failure}}))
