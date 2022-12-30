(ns tau.core
  (:require
   ["react-dom/client" :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tau.events :as events]
   [tau.routes :as routes]
   [tau.subs]
   [tau.views :as views]))

(defonce root (rdom/createRoot (.querySelector js/document "#app")))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (routes/start-routes!)
  (.render root (r/as-element [views/app])))

(defn ^:export init
  []
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
