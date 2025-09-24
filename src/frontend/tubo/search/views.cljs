(ns tubo.search.views
  (:require
   ["motion/react" :refer [motion AnimatePresence]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn suggestions-box
  [suggestions]
  (let [show-suggestions? @(rf/subscribe [:search/show-suggestions])]
    [:> AnimatePresence
     (when (and (seq suggestions) show-suggestions?)
       [:> motion.div
        {:animate    {:y 0}
         :initial    {:y 10}
         :transition {:duration 0.1 :ease "easeIn"}
         :exit       {:y 10 :opacity 0}
         :style      {:translateX "-50%"}
         :class      ["flex" "flex-col" "absolute" "overflow-y-auto" "top-full"
                      "md:rounded" "sm:py-2" "bg-neutral-200"
                      "dark:bg-neutral-900" "w-full" "sm:w-[20rem]"
                      "lg:w-[28rem]"
                      "left-1/2" "sm:top-[110%]" "sm:max-h-[87dvh]"
                      "max-h-[calc(100dvh-56px)]"]}
        (for [[i suggestion] (map-indexed vector suggestions)]
          ^{:key i}
          [:div.hover:bg-neutral-300.dark:hover:bg-neutral-800.cursor-pointer.flex.justify-between.items-center
           [:div.flex.items-center.gap-x-4.w-full.py-2.px-4
            {:on-click #(rf/dispatch [:search/submit suggestion])}
            [:i.fa-solid.fa-search]
            [:span.line-clamp-1 suggestion]]
           [:div.py-2.px-4
            {:on-click #(rf/dispatch [:search/fill-query suggestion])}
            [:i.fa-solid.fa-arrow-up-long.-rotate-45]]])])]))

(defn search-form
  []
  (let [service-query     @(rf/subscribe [:search/query])
        show-search-form? @(rf/subscribe [:search/show-form])
        !input            @(rf/subscribe [:search-input])
        suggestions       @(rf/subscribe [:search/service-suggestions])
        service           @(rf/subscribe [:services/current])
        search-filter     @(rf/subscribe [:search/filter])
        settings          @(rf/subscribe [:settings])
        service-id        @(rf/subscribe [:service-id])
        filter-eq         (first (filter #(= search-filter %)
                                         (:content-filters service)))
        default-filter-eq (first (filter #(= (get (:default-filter settings)
                                                  service-id)
                                             %)
                                         (:content-filters service)))
        filter-idx        (let [idx (when (or filter-eq default-filter-eq)
                                      (.indexOf (:content-filters service)
                                                (or filter-eq
                                                    default-filter-eq)))]
                            (if (pos-int? idx) idx 0))]
    [:<>
     [:form.relative.items-center.justify-center.flex-auto.md:flex-0.flex.items-center.justify-center.px-2.w-full
      {:class     (into []
                        (when-not show-search-form? ["hidden" "md:flex"]))
       :on-submit #(do (.preventDefault %)
                       (rf/dispatch [:search/submit (.-value @!input)]))}
      [:div
       {:class ["flex" "items-center" "relative" "flex-auto" "sm:flex-none"
                "bg-white" "dark:bg-neutral-900" "rounded-full" "px-4"
                "border" "border-neutral-00" "dark:border-neutral-800" "h-10"
                "sm:w-[22rem]" "lg:w-[28rem]"]}
       [:button.p-2.md:hidden
        {:type "button" :on-click #(rf/dispatch [:search/cancel])}
        [:i.fa-solid.fa-arrow-left]]
       [:input.bg-transparent.pl-0.pr-12.border-none.focus:ring-transparent.py-0.w-full.flex-auto.h-full
        {:type          "text"
         :ref           #(reset! !input %)
         :default-value service-query
         :on-focus      #(rf/dispatch [:search/focus-search
                                       (.. % -target -value)])
         :on-change     #(rf/dispatch [:search/change-query
                                       (.. % -target -value)])
         :placeholder   "Search"}]
       [:button {:type "submit"} [:i.fa-solid.fa-search]]
       [layout/popover
        (map-indexed
         (fn [i filter]
           {:label    [:span.flex.items-center.gap-x-4
                       [:input
                        {:class           (when-not (= filter-idx i)
                                            ["bg-neutral-200"
                                             "dark:bg-neutral-800"])
                         :style           {:color (utils/get-service-color
                                                   service-id)}
                         :type            "radio"
                         :name            "instance"
                         :default-checked (= filter-idx i)}]
                       (utils/titleize filter)]
            :value    filter
            :on-click #(rf/dispatch [:search/change-filter filter])})
         (:content-filters service))
        :extra-classes ["p-0" "pr-2" "pl-6"]
        :tooltip-classes ["right-0" "top-12" "sm:right-auto" "sm:left-4"]]
       [:button.px-4.absolute
        {:on-click #(rf/dispatch [:search/clear-query])
         :type     "button"
         :class    ["right-[4.5rem]"
                    (when-not (seq service-query) "invisible")]}
        [:i.fa-solid.fa-xmark]]]]
     [suggestions-box suggestions]]))

(defn search
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{{:keys [q serviceId]} :query-params}]
      (let [{:keys [items next-page]} @(rf/subscribe [:search/results])
            service-id                (or @(rf/subscribe [:service-id])
                                          serviceId)
            filter                    @(rf/subscribe [:search/filter])
            service                   @(rf/subscribe [:services/current])
            settings                  @(rf/subscribe [:settings])]
        [layout/content-container
         [:div.flex.w-full.justify-between
          [layout/select
           (or filter (get (:default-filter settings) service-id) "")
           (map (fn [filter] {:label (utils/titleize filter) :value filter})
                (:content-filters service))
           #(rf/dispatch [:search/set-filter (.. % -target -value)])]
          [items/layout-switcher !layout]]
         [items/related-streams items next-page @!layout
          #(rf/dispatch
            [:search/fetch-paginated
             {:query     q
              :id        service-id
              :filter    (or filter
                             (get (:default-filter settings) service-id))
              :next-page next-page}])]]))))
