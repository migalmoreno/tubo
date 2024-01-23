(ns tubo.views.search
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn search
  [{{:keys [q serviceId]} :query-params}]
  (let [{:keys [items next-page] :as search-results} @(rf/subscribe [:search-results])
        next-page-url (:url next-page)
        services @(rf/subscribe [:services])
        service-id @(rf/subscribe [:service-id])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/search-pagination q serviceId next-page-url]))
    [layout/content-container
     [items/related-streams items next-page-url]]))
