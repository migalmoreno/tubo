(ns tubo.search.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn search-form
  []
  (let [!input (atom nil)]
    (fn []
      (let [query             @(rf/subscribe [:search/query])
            show-search-form? @(rf/subscribe [:search/show-form])]
        [:form.relative.text-white.flex.items-center.justify-center.flex-auto.lg:flex-1
         {:class     (when-not show-search-form? "hidden")
          :on-submit #(do (.preventDefault %) (rf/dispatch [:search/submit]))}
         [:div.flex.items-center.relative.flex-auto.lg:flex-none
          [:button.p-2
           {:type "button" :on-click #(rf/dispatch [:search/cancel])}
           [:i.fa-solid.fa-arrow-left]]
          [:input.w-full.lg:w-96.bg-transparent.pl-0.pr-6.m-2.border-none.focus:ring-transparent.placeholder-white
           {:type          "text"
            :ref           #(reset! !input %)
            :default-value query
            :on-change     #(rf/dispatch [:search/change-query
                                          (.. % -target -value)])
            :placeholder   "Search"}]
          [:button.p-3 {:type "submit"} [:i.fa-solid.fa-search]]
          [:button.p-4.absolute.right-8
           {:on-click #(when @!input
                         (set! (.-value @!input) "")
                         (rf/dispatch [:search/clear-query])
                         (.focus @!input))
            :type     "button"
            :class    (when (empty? query) :invisible)}
           [:i.fa-solid.fa-xmark]]]]))))

(defn search
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{{:keys [q serviceId]} :query-params}]
      (let [{:keys [items next-page]} @(rf/subscribe [:search/results])
            next-page-url             (:url next-page)
            service-id                (or @(rf/subscribe [:service-id])
                                          serviceId)
            filter                    @(rf/subscribe [:search/filter])
            service                   @(rf/subscribe [:services/current])
            settings                  @(rf/subscribe [:settings])]
        [layout/content-container
         [:div.flex.w-full.justify-between
          [layout/select-input
           (or filter (get (:default-filter settings) service-id))
           (:content-filters service)
           #(rf/dispatch [:search/set-filter (.. % -target -value)])]
          [items/layout-switcher !layout]]
         [items/related-streams items next-page-url !layout
          #(rf/dispatch
            [:search/fetch-paginated
             {:query         q
              :id            service-id
              :filter        (or filter
                                 (get (:default-filter settings) service-id))
              :next-page-url next-page-url}])]]))))
