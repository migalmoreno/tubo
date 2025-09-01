(ns tubo.core
  (:require
   ["react-dom/client" :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.events]
   [tubo.router :as router]
   [tubo.subs]
   [tubo.views :as views]))

(defonce root (rdom/createRoot (.querySelector js/document "#app")))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (router/start-router!)
  (.render root (r/as-element [(fn [] views/app)])))

(defn ^:export init [] (rf/dispatch-sync [:initialize]) (mount-root))
