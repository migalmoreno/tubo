(ns tau.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [tau.events]
   [tau.routes :as routes]
   [tau.subs]
   [tau.views :as views]))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (rdom/render
   [views/app]
   (.querySelector js/document "#app")))

(defn ^:export init
  []
  (routes/start-routes!)
  (rf/dispatch-sync [:initialize-db])
  (mount-root))
