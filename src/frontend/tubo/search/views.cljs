(ns tubo.search.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn suggestions-box
  [suggestions]
  [:div.bg-neutral-200.dark:bg-neutral-800.sm:py-2.md:rounded.w-full.lg:w-96.flex.flex-col.absolute.overflow-y-auto.top-full
   {:class ["sm:w-11/12" "-translate-x-1/2" "left-1/2"
            "sm:top-[110%]"
            "sm:max-h-[87dvh]" "max-h-[calc(100dvh-56px)]"]}
   (for [[i suggestion] (map-indexed vector suggestions)]
     ^{:key i}
     [:div.hover:bg-neutral-300.dark:hover:bg-neutral-700.cursor-pointer.flex.justify-between.items-center
      [:div.flex.items-center.gap-x-4.w-full.py-2.px-4
       {:on-click #(rf/dispatch [:search/submit suggestion])}
       [:i.fa-solid.fa-search]
       [:span.line-clamp-1 suggestion]]
      [:div.py-2.px-4
       {:on-click #(rf/dispatch [:search/fill-query suggestion])}
       [:i.fa-solid.fa-arrow-up-long.-rotate-45]]])])

(defn search-form
  []
  (let [service-query     @(rf/subscribe [:search/query])
        show-search-form? @(rf/subscribe [:search/show-form])
        !input            @(rf/subscribe [:search-input])
        suggestions       @(rf/subscribe [:search/service-suggestions])
        show-suggestions? @(rf/subscribe [:search/show-suggestions])]
    [:<>
     [:form.relative.text-white.flex.items-center.justify-center.flex-auto.lg:flex-1
      {:class     (when-not show-search-form? "hidden")
       :on-submit #(do (.preventDefault %)
                       (rf/dispatch [:search/submit (.-value @!input)]))}
      [:div.flex.items-center.relative.flex-auto.lg:flex-none
       [:button.p-2
        {:type "button" :on-click #(rf/dispatch [:search/cancel])}
        [:i.fa-solid.fa-arrow-left]]
       [:input.w-full.lg:w-96.bg-transparent.pl-0.pr-6.m-2.border-none.focus:ring-transparent.placeholder-white
        {:type          "text"
         :ref           #(reset! !input %)
         :default-value service-query
         :on-focus      #(rf/dispatch [:search/focus-search
                                       (.. % -target -value)])
         :on-change     #(rf/dispatch [:search/change-query
                                       (.. % -target -value)])
         :placeholder   "Search"}]
       [:button.p-3 {:type "submit"} [:i.fa-solid.fa-search]]
       [:button.p-4.absolute.right-8
        {:on-click #(rf/dispatch [:search/clear-query])
         :type     "button"
         :class    (when (empty? service-query) :invisible)}
        [:i.fa-solid.fa-xmark]]]]
     (when (and (seq suggestions) show-suggestions?)
       [suggestions-box suggestions])]))

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
