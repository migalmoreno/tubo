(ns tubo.config
  (:refer-clojure :exclude [get get-in])
  (:require
   [shadow-env.core :as env]
   #?@(:clj
         [[clojure.java.io :refer [resource]]
          [aero.core :refer [read-config]]])))

#?(:clj
     (defn read-env
       [_]
       (let [cfg (read-config (resource "config.edn"))]
         {:common {}
          :clj    cfg
          :cljs   cfg})))

(declare get)
(env/link get `read-env)

(defn get-in
  [keys]
  (clojure.core/get-in (get (first keys)) (rest keys)))
