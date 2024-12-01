(ns tubo.navigation.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tubo.channel.views :as channel]
   [tubo.kiosks.views :as kiosks]
   [tubo.layout.views :as layout]
   [tubo.services.views :as services]
   [tubo.stream.views :as stream]
   [tubo.playlist.views :as playlist]))

(defn search-form
  []
  (let [!query (r/atom "")
        !input (r/atom nil)]
    (fn []
      (let [search-query      @(rf/subscribe [:search/query])
            show-search-form? @(rf/subscribe [:search/show-form])
            service-id        @(rf/subscribe [:service-id])]
        [:form.relative.text-white.flex.items-center.justify-center.flex-auto.lg:flex-1
         {:class     (when-not show-search-form? "hidden")
          :on-submit #(do (.preventDefault %)
                          (when-not (empty? @!query)
                            (rf/dispatch [:navigation/navigate
                                          {:name   :search-page
                                           :params {}
                                           :query  {:q         search-query
                                                    :serviceId service-id}}])))}
         [:div.flex.relative.flex-auto.lg:flex-none
          [:button.mx-2
           {:type "button" :on-click #(rf/dispatch [:search/show-form false])}
           [:i.fa-solid.fa-arrow-left]]
          [:input.w-full.lg:w-96.bg-transparent.py-2.pl-0.pr-6.mx-2.border-none.focus:ring-transparent.placeholder-white
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
           {:on-click #(when @!input
                         (set! (.-value @!input) "")
                         (reset! !query "")
                         (.focus @!input))
            :type     "button"
            :class    (when (empty? @!query) :invisible)}
           [:i.fa-solid.fa-circle-xmark]]]]))))

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
   [layout/focus-overlay #(rf/dispatch [:navigation/toggle-mobile-menu])
    show-mobile-nav?]
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

(defn nav-left-content
  [title]
  (let [show-search-form? @(rf/subscribe [:search/show-form])
        show-queue?       @(rf/subscribe [:queue/show])
        show-main-player? @(rf/subscribe [:main-player/show])]
    [:div.flex.items-center.gap-x-4
     (when-not (or show-queue? show-main-player?)
       [:button.ml-2.invisible.absolute.lg:visible.lg:relative
        [:a.font-bold {:href (rfe/href :homepage)}
         [layout/logo :height 35 :width 35]]])
     (when (and show-queue? (not show-search-form?))
       [:button.text-white.mx-2
        {:on-click #(rf/dispatch [:queue/show false])}
        [:i.fa-solid.fa-arrow-left]])
     (when (and show-main-player? (not show-search-form?))
       [:button.text-white.mx-2
        {:on-click #(rf/dispatch [:bg-player/switch-from-main nil])}
        [:i.fa-solid.fa-arrow-left]])
     (when-not (or show-queue? show-main-player? show-search-form?)
       [:button.text-white.mx-3.lg:hidden
        {:on-click #(rf/dispatch
                     [:navigation/toggle-mobile-menu])}
        [:i.fa-solid.fa-bars]])
     (when-not (or show-queue? show-main-player? show-search-form?)
       [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1.lg:hidden
        title])
     (when (and show-queue? (not show-search-form?))
       [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1
        "Play Queue"])
     (when (and show-main-player? (not show-search-form?))
       [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1
        "Main Player"])]))

(defn nav-right-content
  [{{:keys [kioskId]} :query-params path :path :as match}]
  (let [show-search-form? @(rf/subscribe [:search/show-form])
        show-main-player? @(rf/subscribe [:main-player/show])
        show-queue?       @(rf/subscribe [:queue/show])
        service-id        @(rf/subscribe [:service-id])
        service-color     @(rf/subscribe [:service-color])
        services          @(rf/subscribe [:services])
        settings          @(rf/subscribe [:settings])
        kiosks            @(rf/subscribe [:kiosks])]
    [:div.flex.flex-auto.justify-end.lg:justify-between
     {:class (when show-search-form? :hidden)}
     (when-not (or show-queue? show-main-player?)
       [:div.hidden.lg:flex
        [services/services-dropdown services service-id service-color]
        [kiosks/kiosks-menu
         :kiosks (:available-kiosks kiosks)
         :service-id service-id
         :kiosk-id kioskId
         :default-service (:default-service settings)
         :default-kiosk (:default-kiosk kiosks)
         :path path]])
     [:div.flex.flex-auto.items-center.text-white.justify-end
      [:button.mx-3
       {:on-click #(rf/dispatch [:search/show-form true])}
       [:i.fa-solid.fa-search]]
      (when-not (or show-queue? show-main-player?)
        [:div.lg:hidden
         (case (-> match
                   :data
                   :name)
           :channel-page  [channel/metadata-popover
                           @(rf/subscribe [:channel])]
           :stream-page   [stream/metadata-popover @(rf/subscribe [:stream])]
           :playlist-page [playlist/metadata-popover
                           @(rf/subscribe [:playlist])]
           [:<>])])
      [:a.mx-3.hidden.lg:block
       {:href (rfe/href :settings-page)}
       [:i.fa-solid.fa-cog]]
      [:a.mx-3.hidden.lg:block
       {:href (rfe/href :bookmarks-page)}
       [:i.fa-solid.fa-bookmark]]]]))

(defn navbar
  [{{:keys [kioskId]} :query-params path :path :as match}]
  (let [service-id       @(rf/subscribe [:service-id])
        service-color    @(rf/subscribe [:service-color])
        services         @(rf/subscribe [:services])
        show-mobile-nav? @(rf/subscribe [:navigation/show-mobile-menu])
        settings         @(rf/subscribe [:settings])
        kiosks           @(rf/subscribe [:kiosks])]
    [:nav.sticky.flex.items-center.px-2.h-14.top-0.z-20
     {:style {:background service-color}}
     [:div.flex.flex-auto.items-center
      [mobile-nav show-mobile-nav? service-color services
       (:available-kiosks kiosks)
       :kiosk-id kioskId
       :service-id service-id
       :default-service (:default-service settings)
       :default-kiosk (:default-kiosk kiosks)
       :path path]
      [nav-left-content
       (case (-> match
                 :data
                 :name)
         :channel-page  (:name @(rf/subscribe [:channel]))
         :kiosk-page    (:id @(rf/subscribe [:kiosk]))
         :stream-page   (:name @(rf/subscribe [:stream]))
         :playlist-page (:name @(rf/subscribe [:playlist]))
         nil)]
      [search-form]
      [nav-right-content match]]]))
