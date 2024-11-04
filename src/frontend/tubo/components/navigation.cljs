(ns tubo.components.navigation
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.layout :as layout]
   [tubo.kiosks.views :as kiosks]
   [tubo.search.views :as search]
   [tubo.services.views :as services]))

(defn mobile-nav-item
  [route icon label & {:keys [new-tab? active?]}]
  [:li.px-5.py-2
   [:a.flex {:href route :target (when new-tab? "_blank")}
    [:div.w-6.flex.justify-center.items-center.mr-4
     (conj icon {:class ["text-neutral-600" "dark:text-neutral-300"]})]
    [:span {:class (when active? "font-bold")} label]]])

(defn mobile-nav
  [show-mobile-nav? service-color services available-kiosks &
   {:keys [service-id] :as kiosk-args}]
  [:<>
   [layout/focus-overlay #(rf/dispatch [:toggle-mobile-nav]) show-mobile-nav?]
   [:div.fixed.overflow-x-hidden.min-h-screen.w-60.top-0.transition-all.ease-in-out.delay-75.bg-white.dark:bg-neutral-900.z-20
    {:class [(if show-mobile-nav? "left-0" "left-[-245px]")]}
    [:div.flex.justify-center.py-4.items-center.text-white
     {:style {:background service-color}}
     [layout/logo :height 75 :width 75]
     [:h3.text-3xl.font-bold "Tubo"]]
    [services/services-dropdown services service-id service-color]
    [:div.relative.py-4
     [:ul.flex.flex-col
      (for [[i kiosk] (map-indexed vector available-kiosks)]
        ^{:key i}
        [mobile-nav-item
         (rfe/href :kiosk-page nil {:serviceId service-id :kioskId kiosk})
         [:i.fa-solid.fa-fire] kiosk
         :active? (kiosks/kiosk-active? (assoc kiosk-args :kiosk kiosk))])]]
    [:div.relative.dark:border-neutral-800.border-gray-300.pt-4
     {:class "border-t-[1px]"}
     [:ul.flex.flex-col
      [mobile-nav-item (rfe/href :bookmarks-page) [:i.fa-solid.fa-bookmark]
       "Bookmarks"]
      [mobile-nav-item (rfe/href :settings-page) [:i.fa-solid.fa-cog]
       "Settings"]]]]])

(defn navbar
  [{{:keys [kioskId]} :query-params path :path}]
  (let [service-id                               @(rf/subscribe [:service-id])
        service-color                            @(rf/subscribe
                                                   [:service-color])
        services                                 @(rf/subscribe [:services])
        show-mobile-nav?                         @(rf/subscribe
                                                   [:show-mobile-nav])
        show-search-form?                        @(rf/subscribe
                                                   [:show-search-form])
        {:keys [default-service]}                @(rf/subscribe [:settings])
        {:keys [available-kiosks default-kiosk]} @(rf/subscribe [:kiosks])]
    [:nav.sticky.flex.items-center.px-2.h-14.top-0.z-20
     {:style {:background service-color}}
     [:div.flex.flex-auto.items-center
      [:button.ml-2.invisible.absolute.lg:visible.lg:relative
       [:a.font-bold {:href (rfe/href :homepage)}
        [layout/logo :height 35 :width 35]]]
      [:button.text-white.mx-3.lg:hidden
       {:on-click #(rf/dispatch [:toggle-mobile-nav])}
       [:i.fa-solid.fa-bars]]
      [search/search-form]
      [:div.flex.flex-auto.justify-end.lg:justify-between
       {:class (when show-search-form? :hidden)}
       [:div.hidden.lg:flex
        [services/services-dropdown services service-id service-color]
        [kiosks/kiosks-menu
         :kiosks available-kiosks
         :service-id service-id
         :kiosk-id kioskId
         :default-service default-service
         :default-kiosk default-kiosk
         :path path]]
       [:div.flex.items-center.text-white.justify-end
        (when-not show-search-form?
          [:button.mx-3
           {:on-click #(rf/dispatch [:search/show-form true])}
           [:i.fa-solid.fa-search]])
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href :settings-page)}
         [:i.fa-solid.fa-cog]]
        [:a.mx-3.hidden.lg:block
         {:href (rfe/href :bookmarks-page)}
         [:i.fa-solid.fa-bookmark]]]
       [mobile-nav show-mobile-nav? service-color services available-kiosks
        :kiosk-id kioskId
        :service-id service-id
        :default-service default-service
        :default-kiosk default-kiosk
        :path path]]]]))
