(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :refer [clear go halt prep init reset reset-all] :as repl]
   [tubo.system :refer [config]]))

(repl/set-prep! #(ig/expand config))
