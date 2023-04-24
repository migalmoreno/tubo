(ns tubo.core
  (:require
   ["react-dom/client" :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.events :as events]
   [tubo.routes :as routes]
   [tubo.subs]
   [tubo.views :as views]))

(defonce root (rdom/createRoot (.querySelector js/document "#app")))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (routes/start-routes!)
  (.render root (r/as-element [(fn [] views/app)])))

(defn ^:export init
  []
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
