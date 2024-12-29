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
   [tubo.playlist.views :as playlist]
   [tubo.bg-player.views :as bg-player]))

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
         [:div.flex.items-center.relative.flex-auto.lg:flex-none
          [:button.p-2
           {:type "button" :on-click #(rf/dispatch [:search/show-form false])}
           [:i.fa-solid.fa-arrow-left]]
          [:input.w-full.lg:w-96.bg-transparent.pl-0.pr-6.m-2.border-none.focus:ring-transparent.placeholder-white
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
          [:button.p-3 {:type "submit"} [:i.fa-solid.fa-search]]
          [:button.p-4.absolute.right-8
           {:on-click #(when @!input
                         (set! (.-value @!input) "")
                         (reset! !query "")
                         (.focus @!input))
            :type     "button"
            :class    (when (empty? @!query) :invisible)}
           [:i.fa-solid.fa-xmark]]]]))))

(defn nav-left-content
  [title]
  (let [show-search-form? @(rf/subscribe [:search/show-form])
        show-queue?       @(rf/subscribe [:queue/show])
        show-main-player? @(rf/subscribe [:main-player/show])]
    [:div.flex.items-center.gap-x-4
     (when-not (or show-queue? show-main-player?)
       [:button.ml-2.invisible.absolute.lg:visible.lg:relative
        [:a.font-bold {:href (rfe/href :homepage)}
         [layout/logo :height 25 :width 25]]])
     (cond (and show-main-player? (not show-search-form?))
           [:button.text-white.mx-2
            {:on-click #(rf/dispatch [:bg-player/switch-from-main nil])}
            [:i.fa-solid.fa-arrow-left]]
           (and show-queue? (not show-search-form?))
           [:button.text-white.mx-2
            {:on-click #(rf/dispatch [:queue/show false])}
            [:i.fa-solid.fa-arrow-left]])
     (when (and (not show-queue?)
                (not show-main-player?)
                (not show-search-form?))
       [:button.text-white.mx-3.lg:hidden
        {:on-click #(rf/dispatch
                     [:navigation/show-mobile-menu])}
        [:i.fa-solid.fa-bars]])
     (cond (and (not show-queue?)
                (not show-main-player?)
                (not show-search-form?))
           [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1.lg:hidden
            title]
           (and show-main-player? (not show-search-form?))
           [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1
            "Main Player"]
           (and show-queue? (not show-search-form?))
           [:h1.text-white.text-lg.sm:text-xl.font-bold.line-clamp-1
            "Play Queue"])]))

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
      [:div.xs:hidden
       (case (-> match
                 :data
                 :name)
         :channel-page  [channel/metadata-popover
                         @(rf/subscribe [:channel])]
         :stream-page   [stream/metadata-popover @(rf/subscribe [:stream])]
         :playlist-page [playlist/metadata-popover
                         @(rf/subscribe [:playlist])]
         (cond show-main-player? [stream/metadata-popover
                                  @(rf/subscribe [:stream])]
               show-queue?       [bg-player/popover
                                  @(rf/subscribe [:queue/current])]
               :else             [:<>]))]
      [:a.mx-3.hidden.lg:block
       {:href (rfe/href :settings-page)}
       [:i.fa-solid.fa-cog]]
      [:a.mx-3.hidden.lg:block
       {:href (rfe/href :bookmarks-page)}
       [:i.fa-solid.fa-bookmark]]
      [:a.mx-3.hidden.lg:block
       {:href (rfe/href :about-page)}
       [:i.fa-solid.fa-circle-info]]]]))

(defn navbar
  [match]
  (let [service-color @(rf/subscribe [:service-color])]
    [:nav.sticky.flex.items-center.px-2.h-14.top-0.z-20
     {:style {:background service-color}}
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
      [search-form]
      [nav-right-content match]]]))

(defn mobile-menu-item
  [route icon label & {:keys [new-tab? active?]}]
  [:li.hover:bg-neutral-200.dark:hover:bg-neutral-800
   [:a.flex.gap-x-4.p-4 {:href route :target (when new-tab? "_blank")}
    [:div.w-6.flex.justify-center.items-center
     (conj icon {:class ["text-neutral-600" "dark:text-neutral-300"]})]
    [:span {:class (when active? "font-bold")} label]]])

(defn mobile-menu
  [{{:keys [kioskId]} :query-params path :path}]
  (let [service-id       @(rf/subscribe [:service-id])
        service-color    @(rf/subscribe [:service-color])
        services         @(rf/subscribe [:services])
        show-mobile-nav? @(rf/subscribe [:navigation/show-mobile-menu])
        kiosks           @(rf/subscribe [:kiosks])
        settings         @(rf/subscribe [:settings])]
    [:div.fixed.min-h-screen.w-60.top-0.bg-neutral-100.dark:bg-neutral-900.transition-all.ease-in-out.delay-75.z-30
     {:class [(if show-mobile-nav? "left-0" "left-[-245px]")]}
     [:div.flex.justify-center.items-center.py-8.gap-x-4
      {:style {:background service-color}}
      [layout/logo :height 50 :width 50]
      [:h3.text-white.text-3xl.font-semibold "Tubo"]]
     [services/services-dropdown services service-id service-color]
     [:div.relative.py-2
      [:ul.flex.flex-col
       (for [[i kiosk] (map-indexed vector (:available-kiosks kiosks))]
         ^{:key i}
         [mobile-menu-item
          (rfe/href :kiosk-page nil {:serviceId service-id :kioskId kiosk})
          [:i.fa-solid.fa-fire] kiosk
          :active?
          (kiosks/kiosk-active?
           :kiosk-id        kioskId
           :service-id      service-id
           :default-service (:default-service settings)
           :default-kiosk   (or (get (:default-kiosk settings) service-id)
                                (:default-kiosk kiosks))
           :path            path
           :kiosk           kiosk)])]]
     [:ul.flex.flex-col.border-t.dark:border-neutral-800.border-gray-300.py-2
      [mobile-menu-item (rfe/href :bookmarks-page) [:i.fa-solid.fa-bookmark]
       "Bookmarks"]
      [mobile-menu-item (rfe/href :settings-page) [:i.fa-solid.fa-cog]
       "Settings"]
      [mobile-menu-item (rfe/href :about-page) [:i.fa-solid.fa-circle-info]
       "About & FAQ"]]]))
