(ns tubo.navigation.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [svgreq.core :as svg]
   [tubo.channel.views :as channel]
   [tubo.kiosks.views :as kiosks]
   [tubo.layout.views :as layout]
   [tubo.search.views :as search]
   [tubo.stream.views :as stream]
   [tubo.playlist.views :as playlist]
   [tubo.bg-player.views :as bg-player]
   [tubo.utils :as utils]
   [clojure.string :as str]))

(defn logo
  []
  [:a.justify-center.items-center.gap-x-2.px-2.flex
   {:href (rfe/href :homepage)}
   [layout/logo :height 20 :width 20]
   [:h3.text-xl.font-extrabold "Tubo"]])

(defn nav-left-content
  [title]
  (let [show-search-form? @(rf/subscribe [:search/show-form])
        show-queue?       @(rf/subscribe [:queue/show])
        show-main-player? @(rf/subscribe [:main-player/show])
        show-sidebar?     @(rf/subscribe [:navigation/show-sidebar])
        sidebar-state     @(rf/subscribe
                            [:navigation/sidebar-match-media-state])]
    [:div.flex.items-center.gap-x-4
     (when (and (not show-queue?)
                (not show-main-player?))
       [:div.flex.items-center.justify-center.text-lg.pl-8
        {:class (when show-search-form? ["hidden" "md:flex" "pl-0"])}
        [:button
         {:on-click #(rf/dispatch [:navigation/show-mobile-menu])
          :class    (if (false? show-sidebar?) "lg:block" "lg:hidden")}
         [:i.fa-solid.fa-bars]]
        [:button.hidden.lg:flex
         {:class    (when (false? show-sidebar?) "lg:hidden")
          :on-click #(rf/dispatch [:navigation/show-sidebar
                                   (cond (= show-sidebar? :minimized) :expanded
                                         (and (= sidebar-state :minimized)
                                              (nil? show-sidebar?))
                                         :expanded
                                         (and (= sidebar-state :expanded)
                                              (nil? show-sidebar?))
                                         :minimized
                                         :else :minimized)])}
         [:i.fa-solid.fa-bars]]])
     (when (and (not show-queue?) (not show-main-player?))
       [:div {:class (if show-search-form? "hidden md:flex" "hidden sm:flex")}
        [logo]])
     (cond (and show-main-player? (not show-search-form?))
           [:button.pl-8
            {:on-click #(rf/dispatch [:bg-player/switch-from-main nil])}
            [:i.fa-solid.fa-arrow-left]]
           (and show-queue? (not show-search-form?))
           [:button.pl-8
            {:on-click #(rf/dispatch [:queue/show false])}
            [:i.fa-solid.fa-arrow-left]])
     [:div.font-extrabold.text-lg.sm:text-xl
      (cond (and (not show-queue?)
                 (not show-main-player?)
                 (not show-search-form?))
            [:h1.pl-4.line-clamp-1.sm:hidden title]
            (and show-main-player? (not show-search-form?))
            [:h1.pl-4.whitespace-nowrap "Main Player"]
            (and show-queue? (not show-search-form?))
            [:h1.pl-4.whitespace-nowrap "Play Queue"])]]))

(def theme-tooltip-items
  [{:label    "Light"
    :icon     [:i.fa-solid.fa-sun]
    :on-click #(rf/dispatch [:settings/change [:theme]
                             "light"])}
   {:label    "Dark"
    :icon     [:i.fa-solid.fa-moon]
    :on-click #(rf/dispatch [:settings/change [:theme]
                             "dark"])}
   {:label    "Device"
    :icon     [:i.fa-solid.fa-laptop]
    :on-click #(rf/dispatch [:settings/change [:theme]
                             "auto"])}])

(defn nav-right-content
  [match]
  (let [show-search-form? @(rf/subscribe [:search/show-form])
        show-main-player? @(rf/subscribe [:main-player/show])
        show-queue?       @(rf/subscribe [:queue/show])
        user              @(rf/subscribe [:auth/user])]
    [:div.flex.justify-end.flex-auto
     [:div.flex.items-center.justify-end
      (when-not show-search-form?
        [:button.px-4.md:hidden
         {:on-click #(rf/dispatch [:search/activate true])}
         [:i.fa-solid.fa-search]])
      [:div.xs:hidden
       (when-not show-search-form?
         (case (get-in match [:data :name])
           :channel-page  [channel/metadata-popover
                           @(rf/subscribe [:channel])]
           :stream-page   [stream/metadata-popover @(rf/subscribe [:stream])]
           :playlist-page [playlist/metadata-popover
                           @(rf/subscribe [:playlist])]
           (cond show-main-player? [stream/metadata-popover
                                    @(rf/subscribe [:stream])]
                 show-queue?       [bg-player/popover
                                    @(rf/subscribe [:queue/current])]
                 :else             [:<>])))]
      [:div.hidden.md:flex
       [layout/popover
        [{:label     (str "Theme: "
                          (str/capitalize (:theme @(rf/subscribe
                                                    [:settings]))))
          :icon      (case (:theme @(rf/subscribe [:settings]))
                       "light" [:i.fa-solid.fa-sun]
                       "dark"  [:i.fa-solid.fa-moon]
                       "auto"  [:i.fa-solid.fa-laptop])
          :subschema theme-tooltip-items}
         {:label "Settings"
          :icon  [:i.fa-solid.fa-cog]
          :link  {:route (rfe/href :settings-page)}}]
        :extra-classes ["p-0" "px-5" "z-30"]
        :tooltip-classes ["right-5" "top-8" "w-44"]]]
      [:div.hidden.md:flex
       [layout/popover
        (into (if-not user
                [{:label "Register"
                  :icon  [:i.fa-solid.fa-user-plus]
                  :link  {:route (rfe/href :register-page)}}
                 {:label "Log In"
                  :icon  [:i.fa-solid.fa-right-to-bracket]
                  :link  {:route (rfe/href :login-page)}}]
                [])
              [(when user
                 {:label    "Log Out"
                  :icon     [:i.fa-solid.fa-right-to-bracket
                             {:class "rotate-180"}]
                  :on-click #(rf/dispatch [:auth/logout])})])
        :extra-classes ["p-0" "px-5" "z-30"]
        :tooltip-classes ["right-5" "top-8" "w-32"]
        :icon [:i.fa-solid.fa-circle-user]]]]]))

(defn navbar
  [match]
  [:nav.sticky.flex.items-center.h-14.top-0.z-20.backdrop-blur-md
   {:class "bg-neutral-100/90 dark:bg-neutral-950/90"}
   [:div.flex.flex-auto.items-center
    [nav-left-content
     (case (-> match
               :data
               :name)
       :channel-page  (:name @(rf/subscribe [:channel]))
       :kiosk-page    (:id @(rf/subscribe [:kiosk]))
       :stream-page   (:name @(rf/subscribe [:stream]))
       :playlist-page (:name @(rf/subscribe [:playlist]))
       nil)]
    [search/search-form]
    [:div.w-24.hidden.md:block]
    [nav-right-content match]]])

(defn sidebar-item
  [route icon label &
   {:keys [new-tab? icon-attrs on-click extra-classes always-expanded?]
    :or   {extra-classes ["hover:bg-neutral-200" "dark:hover:bg-neutral-900"]}}]
  (let [sidebar-minimized? (and (not always-expanded?)
                                @(rf/subscribe
                                  [:navigation/sidebar-minimized]))]
    [:li.cursor-pointer.transition-colors.ease-in-out.delay-50.rounded-xl
     {:class (into (if sidebar-minimized? ["mx-1"] ["mx-2" "pl-5" "w-64"])
                   extra-classes)}
     [:a.flex.gap-4
      {:href     route
       :on-click on-click
       :target   (when new-tab? "_blank")
       :class    (if sidebar-minimized?
                   "flex-col items-center p-3"
                   "py-3")}
      [:div.w-6.flex.justify-center.items-center
       (if (vector? icon)
         (conj icon
               (merge {:class ["text-neutral-600" "dark:text-neutral-300"]}
                      icon-attrs))
         icon)]
      [:span.text-sm.whitespace-nowrap
       {:class [(when sidebar-minimized? "text-xs")]} label]]]))

(defn services-menu
  [{{:keys [kioskId]} :query-params path :path} always-expanded?]
  (let [services           @(rf/subscribe [:services])
        service-id         @(rf/subscribe [:service-id])
        kiosks             @(rf/subscribe [:kiosks])
        settings           @(rf/subscribe [:settings])
        sidebar-minimized? (and (not always-expanded?)
                                @(rf/subscribe
                                  [:navigation/sidebar-minimized]))]
    [:div.flex.flex-col.gap-y-2.py-2.min-w-full
     (for [[i service] (map-indexed vector services)]
       ^{:key i}
       [:ul.flex.flex-col.justify-center
        [sidebar-item nil
         (case (:id service)
           0 [:i.fa-brands.fa-youtube]
           1 [:i.fa-brands.fa-soundcloud]
           2 (r/create-element
              (svg/embed "./resources/public/icons"
                         "media_gadse"
                         nil)
              (js-obj "className" "fill-neutral-600 dark:fill-neutral-300"
                      "height"    "15px"
                      "width"     "15px"))
           3 (r/create-element
              (svg/embed "./resources/public/icons"
                         "peertube"
                         nil)
              (js-obj "className" (when (not= service-id i)
                                    "fill-neutral-600 dark:fill-neutral-300")
                      "fill"      (when (= service-id i)
                                    (utils/get-service-color i))
                      "height"    "15px"
                      "width"     "15px"))
           4 [:i.fa-brands.fa-bandcamp])
         (get-in service [:info :name])
         :on-click #(rf/dispatch [:kiosks/change-page i])
         :always-expanded? always-expanded?
         :icon-attrs
         {:style (when (= service-id i) {:color (utils/get-service-color i)})}
         :extra-classes
         (if (= i service-id)
           ["font-bold" "bg-neutral-200" "dark:bg-neutral-900"]
           ["hover:bg-neutral-200" "dark:hover:bg-neutral-900"])]
        (when (and (= service-id i) (not sidebar-minimized?))
          [:ul.flex.flex-col.list-none.gap-y-2.pt-2.border-l.border-neutral-300.dark:border-neutral-800.ml-6
           (for [[j kiosk] (map-indexed vector (:available-kiosks kiosks))
                 :let      [active? (kiosks/kiosk-active?
                                     :kiosk-id        kioskId
                                     :service-id      service-id
                                     :default-service (:default-service
                                                       settings)
                                     :default-kiosk   (or (get
                                                           (:default-kiosk
                                                            settings)
                                                           service-id)
                                                          (:default-kiosk
                                                           kiosks))
                                     :path            path
                                     :kiosk           kiosk)]]
             ^{:key j}
             [sidebar-item
              (rfe/href :kiosk-page nil {:serviceId service-id :kioskId kiosk})
              [:i.fa-solid.fa-fire]
              kiosk
              :icon-attrs
              {:style (when (and active? (not= i 2))
                        {:color (utils/get-service-color i)})}
              :always-expanded? always-expanded?
              :extra-classes
              ["!pl-2" (when active? "font-bold")]])])])]))

(defn tools-menu
  [always-expanded?]
  (let [user               @(rf/subscribe [:auth/user])
        sidebar-minimized? @(rf/subscribe [:navigation/sidebar-minimized])
        show-mobile-menu?  @(rf/subscribe [:navigation/show-mobile-menu])]
    [:ul.flex.flex-col.border-t.dark:border-neutral-800.py-2.gap-y-2
     {:class "border-gray-400/50"}
     [sidebar-item (rfe/href :bookmarks-page) [:i.fa-solid.fa-bookmark]
      "Bookmarks" :always-expanded? always-expanded?]
     [sidebar-item (rfe/href :settings-page) [:i.fa-solid.fa-cog] "Settings"
      :always-expanded? always-expanded?]
     (when show-mobile-menu?
       [layout/popover
        (map #(assoc % :hide-bg-overlay-on-click? false) theme-tooltip-items)
        :extra-classes ["p-0"]
        :tooltip-classes ["right-5" "top-8" "w-44"]
        :responsive? false
        :icon
        [sidebar-item nil
         (case (:theme @(rf/subscribe [:settings]))
           "light" [:i.fa-solid.fa-sun]
           "dark"  [:i.fa-solid.fa-moon]
           "auto"  [:i.fa-solid.fa-laptop])
         (str "Theme: "
              (str/capitalize (:theme @(rf/subscribe [:settings]))))
         :always-expanded? always-expanded?]])
     (when (or show-mobile-menu? (not sidebar-minimized?))
       (if user
         [sidebar-item nil [:i.fa-solid.fa-user] "Log Out" :always-expanded?
          always-expanded? :on-click #(rf/dispatch [:auth/logout])]
         [:<>
          [sidebar-item (rfe/href :register-page) [:i.fa-solid.fa-user-plus]
           "Register" :always-expanded?
           always-expanded?]
          [sidebar-item (rfe/href :login-page) [:i.fa-solid.fa-right-to-bracket]
           "Log In" :always-expanded?
           always-expanded?]]))
     [sidebar-item (rfe/href :about-page) [:i.fa-solid.fa-circle-info]
      "About" :always-expanded? always-expanded?]]))

(defn sidebar
  [match]
  (let [sidebar-minimized? @(rf/subscribe [:navigation/sidebar-minimized])
        sidebar-shown      @(rf/subscribe [:navigation/sidebar-shown])]
    [:div.sticky.top-14.transition-all.ease-in-out.delay-75.z-10
     {:class (into (into ["h-[calc(100dvh-56px)]"]
                         (if sidebar-shown ["hidden" "md:flex"] ["hidden"]))
                   (if sidebar-minimized?
                     ["min-w-20 w-20 max-w-20"]
                     ["min-w-80 w-80 max-w-80"]))}
     [:div.overflow-auto.scrollbar-none.justify-between.flex.flex-col
      [services-menu match]
      [tools-menu]]]))

(defn mobile-menu
  [match]
  (let [show-mobile-menu? @(rf/subscribe [:navigation/show-mobile-menu])]
    [:div.fixed.h-screen.w-80.top-0.bg-neutral-100.dark:bg-neutral-950.transition-all.ease-in-out.delay-75.z-30.flex.flex-col
     {:class [(if show-mobile-menu? "left-0" "-left-80")]}
     [:div.flex.items-center.h-14.pl-8.gap-x-6
      [:button.text-lg
       {:on-click #(rf/dispatch [:navigation/hide-mobile-menu])}
       [:i.fa-solid.fa-bars]]
      [logo]]
     [:div.flex.flex-col.justify-between.flex-auto.overflow-auto.scrollbar-none
      [services-menu match true]
      [tools-menu true]]]))
