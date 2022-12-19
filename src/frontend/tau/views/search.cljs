(ns tau.views.search
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]))

(defn search-result
  [title author url thumbnail id]
  [:div.w-56.h-64.my-2 {:key id}
   [:div.p-5.border.rounded.border-slate-900.m-2.bg-slate-600.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:a.overflow-hidden {:href (rfe/href :tau.routes/stream {} {:url url}) :title title}
     [:div.flex.justify-center.min-w-full.py-3.box-border
      [:div.h-28.min-w-full.flex.justify-center
       [:img.rounded.object-cover.max-h-full {:src thumbnail}]]]
     [:div.overflow-hidden
      [:h1.text-gray-300.font-bold author]
      [:h1 title]]]]])

(defn search
  [m]
  (let [search-results (rf/subscribe [:search-results])
        services (rf/subscribe [:services])
        service-id (rf/subscribe [:service-id])]
    [:div.text-gray-300.text-center.py-5.relative
     [:h2 (str "Showing search results for: \"" (-> m :query-params :q) "\"")]
     [:h1 (str "Number of search results: " (count (:items @search-results)))]
     ;; TODO: Create loadable component that wraps other components that need to fetch from API
     ;; or use a :loading key to show a spinner component instead
     (if (empty? @search-results)
       [:p "Loading"]
       [:div.flex.justify-center.align-center.flex-wrap
        (for [[i result] (map-indexed vector (:items @search-results))]
          ;; TODO: Add a component per result type
          [search-result
           (:name result)
           (:upload-author result)
           (:url result)
           (:thumbnail-url result)
           i])])]))
