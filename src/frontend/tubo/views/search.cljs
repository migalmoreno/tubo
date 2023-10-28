(ns tubo.views.search
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.loading :as loading]
   [tubo.events :as events]))

(defn search
  [{{:keys [q serviceId]} :query-params}]
  (let [{:keys [items next-page] :as search-results} @(rf/subscribe [:search-results])
        next-page-url (:url next-page)
        services @(rf/subscribe [:services])
        service-id @(rf/subscribe [:service-id])
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/search-pagination q serviceId next-page-url]))
    [:div.flex.flex-col.items-center.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full {:class "lg:w-4/5 xl:w-3/5"}
        [items/related-streams items next-page-url]])]))
