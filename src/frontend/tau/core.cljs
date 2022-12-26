(ns tau.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [tau.events :as events]
   [tau.routes :as routes]
   [tau.subs]
   [tau.views :as views]))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (routes/start-routes!)
  (rdom/render
   [views/app]
   (.querySelector js/document "#app")))

(defn ^:export init
  []
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
