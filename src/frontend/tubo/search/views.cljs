(ns tubo.search.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]))

(defn search-form
  []
  (let [!query (r/atom "")
        !input (r/atom nil)]
    (fn []
      (let [search-query      @(rf/subscribe [:search-query])
            show-search-form? @(rf/subscribe [:show-search-form])
            service-id        @(rf/subscribe [:service-id])]
        [:form.relative.flex.items-center.text-white.ml-4
         {:class     (when-not show-search-form? "hidden")
          :on-submit #(do (.preventDefault %)
                          (when-not (empty? @!query)
                            (rf/dispatch [:navigate
                                          {:name   :search-page
                                           :params {}
                                           :query  {:q         search-query
                                                    :serviceId service-id}}])))}
         [:div.flex
          [:button.mx-2
           {:on-click #(rf/dispatch [:search/show-form false]) :type "button"}
           [:i.fa-solid.fa-arrow-left]]
          [:input.w-full.sm:w-96.bg-transparent.py-2.pl-0.pr-6.mx-2.border-none.focus:ring-transparent.placeholder-white
           {:type          "text"
            :ref           #(do (reset! !input %)
                                (when %
                                  (.focus %)))
            :default-value @!query
            :on-change     #(let [input (.. % -target -value)]
                              (when-not (empty? input)
                                (rf/dispatch [:search/change-query input]))
                              (reset! !query input))
            :placeholder   "Search"}]
          [:button.mx-4 {:type "submit"} [:i.fa-solid.fa-search]]
          [:button.mx-4.text-xs.absolute.right-8.top-3
           {:type     "button"
            :on-click #(when @!input
                         (set! (.-value @!input) "")
                         (reset! !query "")
                         (.focus @!input))
            :class    (when (empty? @!query) :invisible)}
           [:i.fa-solid.fa-circle-xmark]]]]))))

(defn search
  [{{:keys [q serviceId]} :query-params}]
  (let [{:keys [items next-page]} @(rf/subscribe [:search-results])
        next-page-url             (:url next-page)
        service-id                (or @(rf/subscribe [:service-id]) serviceId)
        scrolled-to-bottom?       @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [:search/fetch-paginated q service-id next-page-url]))
    [layout/content-container
     [items/related-streams items next-page-url]]))
