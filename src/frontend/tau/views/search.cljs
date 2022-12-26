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
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/scroll-search-pagination q serviceId next-page-url]))
    [:div.flex.flex-col.text-gray-300.h-box-border.flex-auto
     [:div.flex.flex-col.items-center.w-full.pt-4.flex-initial
      [:h2 (str "Showing search results for: \"" q "\"")]
      [:h1 (str "Number of search results: " (count items))]]
     (if page-loading?
       [loading/page-loading-icon service-color]
       [:div.flex.flex-col
        [:div.flex.justify-center.align-center.flex-wrap.flex-auto
         (for [[i item] (map-indexed vector items)]
           (cond
             (:duration item) [items/stream-item i item]
             (:subscriber-count item) [items/channel-item i item]
             (:stream-count item) [items/playlist-item i item]))
         (when-not (empty? next-page-url)
           [loading/pagination-loading-icon service-color pagination-loading?])]])]))
