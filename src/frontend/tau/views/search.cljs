(ns tau.views.search
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.events :as events]))

(defn search
  [{{:keys [q serviceId]} :query-params}]
  (let [{:keys [items next-page] :as search-results} @(rf/subscribe [:search-results])
        next-page-url (:url next-page)
        services @(rf/subscribe [:services])
        service-id @(rf/subscribe [:service-id])
        service-color @(rf/subscribe [:service-color])
        page-scroll @(rf/subscribe [:page-scroll])
        page-loading? @(rf/subscribe [:show-page-loading])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/search-pagination q serviceId next-page-url]))
    [:div.flex.flex-col.items-center.flex-auto
     [:div.flex.flex-col.items-center.w-full.pt-4.flex-initial
      [:h2 (str "Showing search results for: \"" q "\"")]
      [:h1 (str "Number of search results: " (count items))]]
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full {:class "lg:w-4/5"}
        [items/related-streams items next-page-url]])]))
