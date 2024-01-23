(ns tubo.components.navigation
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.events :as events]
   [tubo.routes :as routes]))

(defn navigation-buttons [service-color]
  [:div.flex.items-center.text-white.ml-4
   [:button.mx-2.outline-none.focus:ring-transparent
    {:on-click #(rf/dispatch [::events/history-go -1])}
    [:i.fa-solid.fa-arrow-left]]
   [:button.mx-2.outline-none.focus:ring-transparent
    {:on-click #(rf/dispatch [::events/history-go 1])}
    [:i.fa-solid.fa-arrow-right]]])

(defn search-form []
  (let [!query (r/atom "")
        !input (r/atom nil)]
    (fn []
      (let [search-query      @(rf/subscribe [:search-query])
            show-search-form? @(rf/subscribe [:show-search-form])
            service-id        @(rf/subscribe [:service-id])]
        [:form.relative.flex.items-center.text-white.ml-4
         {:class (when-not show-search-form? "hidden")
          :on-submit
          (fn [e]
            (.preventDefault e)
            (when-not (empty? @!query)
              (rf/dispatch [::events/navigate
                            {:name   ::routes/search
                             :params {}
                             :query  {:q search-query :serviceId service-id}}])))}
         [:div.flex
          [:button.mx-2
           {:on-click #(rf/dispatch [::events/toggle-search-form])
            :type     "button"}
           [:i.fa-solid.fa-arrow-left]]
          [:input.bg-transparent.border-none.py-2.pr-6.mx-2.focus:ring-transparent.placeholder-white.sm:w-96.w-full
           {:type          "text"
            :ref           #(do (reset! !input %)
                                (when %
                                  (.focus %)))
            :default-value @!query
            :on-change     #(let [input (.. % -target -value)]
                              (when-not (empty? input) (rf/dispatch [::events/change-search-query input]))
                              (reset! !query input))
            :placeholder   "Search"}]
          [:button.mx-4
           {:type "submit"}
           [:i.fa-solid.fa-search]]
          [:button.mx-4.text-xs.absolute.right-8.top-3
           {:type     "button"
            :on-click #(when @!input
                         (set! (.-value @!input) "")
                         (reset! !query "")
                         (.focus @!input))
            :class    (when (empty? @!query) "invisible")}
           [:i.fa-solid.fa-circle-xmark]]]]))))
