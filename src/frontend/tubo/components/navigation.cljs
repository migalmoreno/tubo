(ns tubo.components.navigation
  (:require
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]
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
            :style         {:paddingLeft 0}
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

(defn services-dropdown [services service-id service-color]
  [:div.relative.flex.flex-col.items-center-justify-center.text-white.px-2
   {:style {:background service-color}}
   [:div.w-full.box-border.z-10.lg:z-0
    [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.font-nunito.w-full
     {:on-change #(rf/dispatch [::events/change-service-kiosk (js/parseInt (.. % -target -value))])
      :value     service-id
      :style     {:background "transparent"}}
     (when services
       (for [service services]
         [:option.text-white.bg-neutral-900.border-none
          {:value (:id service) :key (:id service)}
          (-> service :info :name)]))]]
   [:div.flex.items-center.justify-end.absolute.min-h-full.top-0.right-4.lg:right-0.z-0
    [:i.fa-solid.fa-caret-down]]])

(defn kiosk-active?
  [{:keys [kiosk kiosk-id service-id default-service default-kiosk path]}]
  (or (and (= kiosk-id kiosk))
      (and (= path "/kiosk")
           (not kiosk-id)
           (not= (js/parseInt service-id)
                 (:service-id default-service))
           (= default-kiosk kiosk))
      (and (or (= path "/") (= path "/kiosk"))
           (not kiosk-id)
           (= (:default-kiosk default-service) kiosk))))

(defn kiosks-menu
  [{:keys [kiosks service-id] :as kiosk-args}]
  [:ul.flex.items-center.px-4.text-white
   (for [kiosk kiosks]
     [:li.px-3 {:key kiosk}
      [:a {:href  (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                :kioskId   kiosk})
           :class (when (kiosk-active? (assoc kiosk-args :kiosk kiosk))
                    "font-bold")}
       kiosk]])])

(defn mobile-nav-item [route icon label & {:keys [new-tab? active?]}]
  [:li.px-5.py-2
   [:a.flex {:href route :target (when new-tab? "_blank")}
    [:div.w-6.flex.justify-center.items-center.mr-4
     [:i.text-neutral-600.dark:text-neutral-300 {:class icon}]]
    [:span {:class (when active? "font-bold")} label]]])

(defn mobile-nav
  [show-mobile-nav? service-color services available-kiosks {:keys [service-id] :as kiosk-args}]
  [:<>
   [layout/focus-overlay #(rf/dispatch [::events/toggle-mobile-nav]) show-mobile-nav?]
   [:div.fixed.overflow-x-hidden.min-h-screen.w-60.top-0.ease-in-out.delay-75.bg-white.dark:bg-neutral-900
    {:class (str "transition-[right] " (if show-mobile-nav? "right-0" "right-[-245px]"))}
    [:div.flex.justify-center.py-8.items-center.text-white {:style {:background service-color}}
     [layout/logo]
     [:h3.text-3xl.font-bold.px-4.font-roboto "Tubo"]]
    [services-dropdown services service-id service-color]
    [:div.relative.py-4
     [:ul.flex.font-roboto.flex-col
      (for [kiosk available-kiosks]
        ^{:key kiosk}
        [mobile-nav-item
         (rfe/href ::routes/kiosk nil
                   {:serviceId service-id
                    :kioskId   kiosk})
         "fa-solid fa-fire" kiosk
         :active? (kiosk-active? (assoc kiosk-args :kiosk kiosk))])]]
    [:div.relative.dark:border-neutral-800.border-gray-300.pt-4
     {:class "border-t-[1px]"}
     [:ul.flex.flex-col.font-roboto
      [mobile-nav-item (rfe/href ::routes/playlists) "fa-solid fa-bookmark" "Bookmarks"]
      [mobile-nav-item (rfe/href ::routes/settings) "fa-solid fa-cog" "Settings"]
      [mobile-nav-item "https://github.com/migalmoreno/tubo"
       "fa-brands fa-github" "Source" :new-tab? true]]]]])

(defn navbar
  [{{:keys [serviceId kioskId]} :query-params path :path}]
  (let [service-id                               @(rf/subscribe [:service-id])
        service-color                            @(rf/subscribe [:service-color])
        services                                 @(rf/subscribe [:services])
        {:keys [theme default-service]}          @(rf/subscribe [:settings])
        id                                       (js/parseInt (or serviceId service-id))
        show-mobile-nav?                         @(rf/subscribe [:show-mobile-nav])
        show-search-form?                        @(rf/subscribe [:show-search-form])
        {:keys [available-kiosks default-kiosk]} @(rf/subscribe [:kiosks])]
    [:nav.sticky.flex.items-center.px-2.h-14.top-0.z-50.font-nunito
     {:style {:background service-color}}
     [:div.flex.flex-auto.items-center
      [:div.ml-4
       [:a.font-bold
        {:href (rfe/href ::routes/home)}
        [layout/logo]]]
      [search-form]
      [:div {:class (when show-search-form? "hidden")}
       [navigation-buttons service-color]]
      [:div.flex.flex-auto.justify-end.lg:justify-between
       {:class (when show-search-form? "hidden")}
       [:div.hidden.lg:flex
        [services-dropdown services service-id service-color]
        [kiosks-menu
         {:kiosks          available-kiosks
          :service-id      service-id
          :kiosk-id        kioskId
          :default-service default-service
          :default-kiosk   default-kiosk
          :path            path}]]
       [:div.flex.items-center.text-white.justify-end
        (when-not show-search-form?
          [:button.mx-3
           {:on-click #(rf/dispatch [::events/toggle-search-form])}
           [:i.fa-solid.fa-search]])
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href ::routes/settings)}
         [:i.fa-solid.fa-cog]]
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href ::routes/playlists)}
         [:i.fa-solid.fa-bookmark]]
        [:a.mx-3.hidden.lg:block
         {:href "https://github.com/migalmoreno/tubo" :target "_blank"}
         [:i.fa-brands.fa-github]]
        [:button.mx-3.lg:hidden
         {:on-click #(rf/dispatch [::events/toggle-mobile-nav])}
         [:i.fa-solid.fa-bars]]]
       [mobile-nav show-mobile-nav? service-color services available-kiosks
        {:kiosk-id        kioskId
         :service-id      service-id
         :default-service default-service
         :default-kiosk   default-kiosk
         :path    path}]]]]))
