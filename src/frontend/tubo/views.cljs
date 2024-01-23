(ns tubo.views
  (:require
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.components.audio-player :as player]
   [tubo.components.layout :as layout]
   [tubo.components.navigation :as navigation]
   [tubo.components.play-queue :as queue]
   [tubo.events :as events]
   [tubo.routes :as routes]))

(defonce services (rf/dispatch [::events/get-services]))
(defonce kiosks (rf/dispatch [::events/get-kiosks 0]))

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

(defn mobile-nav-item [route icon label & {:keys [new-tab?]}]
  [:li.px-5.py-2
   [:a.flex {:href route :target (when new-tab? "_blank")}
    [:div.w-6.flex.justify-center.items-center.mr-4
     [:i.text-neutral-600.dark:text-neutral-300 {:class icon}]]
    [:span label]]])

(defn mobile-nav
  [show-mobile-nav? service-id service-color services available-kiosks]
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
        ^{:key kiosk} [mobile-nav-item
                       (rfe/href ::routes/kiosk nil
                                 {:serviceId service-id
                                  :kioskId   kiosk})
                       "fa-solid fa-fire" kiosk])]]
    [:div.relative.dark:border-neutral-800.border-gray-300.pt-4
     {:class "border-t-[1px]"}
     [:ul.flex.flex-col.font-roboto
      [mobile-nav-item (rfe/href ::routes/bookmarks) "fa-solid fa-bookmark" "Bookmarks"]
      [mobile-nav-item (rfe/href ::routes/settings) "fa-solid fa-cog" "Settings"]
      [mobile-nav-item "https://github.com/migalmoreno/tubo" "fa-brands fa-github" "Source" :new-tab? true]]]]])

(defn navbar
  [{{:keys [serviceId]} :query-params}]
  (let [service-id                               @(rf/subscribe [:service-id])
        service-color                            @(rf/subscribe [:service-color])
        services                                 @(rf/subscribe [:services])
        {:keys [current-theme]}                  @(rf/subscribe [:settings])
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
      [navigation/search-form]
      [:div {:class (when show-search-form? "hidden")}
       [navigation/navigation-buttons service-color]]
      [:div.flex.flex-auto.justify-end.lg:justify-between
       {:class (when show-search-form? "hidden")}
       [:div.hidden.lg:flex
        [services-dropdown services service-id service-color]
        [:ul.flex.items-center.px-4.text-white
         (for [kiosk available-kiosks]
           [:li.px-3 {:key kiosk}
            [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                     :kioskId   kiosk})}
             kiosk]])]]
       [:div.flex.items-center.text-white.justify-end
        (when-not show-search-form?
          [:button.mx-3
           {:on-click (fn []
                        (rf/dispatch [::events/toggle-search-form]))}
           [:i.fa-solid.fa-search]])
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href ::routes/settings)}
         [:i.fa-solid.fa-cog]]
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href ::routes/bookmarks)}
         [:i.fa-solid.fa-bookmark]]
        [:button.mx-3.lg:hidden
         {:on-click #(rf/dispatch [::events/toggle-mobile-nav])}
         [:i.fa-solid.fa-bars]]]
       [mobile-nav show-mobile-nav? service-id service-color services available-kiosks]]]]))

(defn footer
  []
  [:footer
   [:div.bg-neutral-300.dark:bg-black.dark:text-gray-300.p-5.text-center.w-full
    [:div.flex.flex-col.justify-center.items-center
     [:div.flex.items-center.justify-center
      [:div.items-center.font-nunito
       [:a {:href "https://github.com/migalmoreno/tubo" :target "_blank"}
        [:i.fa-brands.fa-github]
        [:span.ml-2.font-bold "Source"]]]]]]])

(defn app
  []
  (let [current-match @(rf/subscribe [:current-match])
        {:keys [current-theme]} @(rf/subscribe [:settings])]
    [:div {:class (when (= current-theme "dark") "dark")}
     [:div.min-h-screen.flex.flex-col.h-full.dark:text-white.dark:bg-neutral-900.relative
      [navbar current-match]
      [:div.flex.flex-col.flex-auto.justify-between.relative.font-nunito
       (when-let [view (-> current-match :data :view)]
         [view current-match])
       [footer]
       [queue/queue]
       [player/player]]]]))
