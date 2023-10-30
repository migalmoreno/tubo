(ns tubo.views
  (:require
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.components.navigation :as navigation]
   [tubo.components.audio-player :as player]
   [tubo.components.play-queue :as queue]
   [tubo.events :as events]
   [tubo.routes :as routes]))

(defonce services (rf/dispatch [::events/get-services]))
(defonce kiosks (rf/dispatch [::events/get-kiosks 0]))

(defn mobile-nav
  [show-mobile-nav? service-id service-color services available-kiosks]
  [:<>
   [:div.w-full.fixed.min-h-screen.right-0.top-0.transition-all.delay-75.ease-in-out
    {:class    "bg-black/50"
     :style    {:visibility (when-not show-mobile-nav? "hidden")
                :opacity (if show-mobile-nav? "1" "0")}
     :on-click #(rf/dispatch [::events/toggle-mobile-nav])}]
   [:div.items-center.fixed.overflow-x-hidden.min-h-screen.w-60.top-0.ease-in-out.delay-75.bg-white.dark:bg-neutral-900
    {:class (str "transition-[right] " (if show-mobile-nav? "right-0" "right-[-245px]"))}
    [:div.flex.justify-center.py-8.items-center.text-white {:style {:background service-color}}
     [:img.mb-1
      {:src   "/images/tubo.png"
       :style {:maxHeight "25px" :maxWidth "40px"}
       :title "Tubo"}]
     [:h3.text-3xl.font-bold.px-4.font-roboto "Tubo"]]
    [:div.relative.flex.flex-col.items-center-justify-center.text-white {:style {:background service-color}}
     [:div.w-full.box-border.z-10
      [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.font-nunito.px-5.w-full
       {:on-change #(rf/dispatch [::events/change-service-kiosk (js/parseInt (.. % -target -value))])
        :value     service-id
        :style     {:background "transparent"}}
       (when services
         (for [service services]
           [:option.text-white.bg-neutral-900.border-none
            {:value (:id service) :key (:id service)}
            (-> service :info :name)]))]]
     [:div.flex.absolute.min-h-full.top-0.right-6.items-center.justify-end.z-0
      [:i.fa-solid.fa-caret-down]]]
    [:div.relative.py-4
     [:ul.flex.font-roboto.flex-col
      (for [kiosk available-kiosks]
        [:li.px-5.py-2 {:key kiosk}
         [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                  :kioskId   kiosk})}
          [:i.fa-solid.fa-fire.text-neutral-600.dark:text-neutral-300]
          [:span.ml-7 kiosk]]])]]
    [:div.relative.dark:border-neutral-800.border-gray-300.pt-4
     {:class "border-t-[1px]"}
     [:ul.flex.font-roboto
      [:li.px-5.py-2
       [:a {:href (rfe/href ::routes/settings)}
        [:i.fa-solid.fa-cog.text-neutral-600.dark:text-neutral-300]
        [:span.ml-7 "Settings"]]]
      [:li.px-5.py-2
       [:a {:href "https://github.com/migalmoreno/tubo" :target "_blank"}
        [:i.fa-brands.fa-github]
        [:span.ml-7 "Source"]]]]]]])

(defn navbar
  [{{:keys [serviceId]} :query-params}]
  (let [!query (r/atom "")
        !input (r/atom nil)]
    (fn [{{:keys [serviceId]} :query-params}]
      (let [service-id                               @(rf/subscribe [:service-id])
            service-color                            @(rf/subscribe [:service-color])
            search-query                             @(rf/subscribe [:search-query])
            services                                 @(rf/subscribe [:services])
            {:keys [current-theme]}                  @(rf/subscribe [:settings])
            id                                       (js/parseInt (or serviceId service-id))
            show-mobile-nav?                         @(rf/subscribe [:show-mobile-nav])
            {:keys [available-kiosks default-kiosk]} @(rf/subscribe [:kiosks])]
        [:nav.flex.px-2.py-2.5.items-center.sticky.top-0.z-50.font-nunito
         {:style {:background service-color}}
         [:div.flex.items-center.justify-between.flex-auto
          [:div.px-2
           [:a.text-white.font-bold
            {:href (rfe/href ::routes/home)}
            [:img.mb-1 {:src   "/images/tubo.png" :style {:maxHeight "25px" :maxWidth "40px"}
                        :title "Tubo"}]]]
          [:form.flex.items-center.relative
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (when-not (empty? @!query)
                           (rf/dispatch [::events/navigate
                                         {:name   ::routes/search
                                          :params {}
                                          :query  {:q search-query :serviceId service-id}}])))}
           [:div.relative
            [:input.bg-transparent.text-white.border-none.py-2.pr-6.mx-2.focus:ring-transparent.placeholder-white.w-40.xs:w-auto
             {:type          "text"
              :ref #(reset! !input %)
              :default-value @!query
              :on-change     #(let [input (.. % -target -value)]
                                (when-not (empty? input) (rf/dispatch [::events/change-search-query input]))
                                (reset! !query input))
              :placeholder   "Search"}]
            [:button.mx-2.text-xs.text-white.absolute.right-0.top-3
             {:type     "button"
              :on-click #(when @!input
                           (set! (.-value @!input) "")
                           (reset! !query "")
                           (.focus @!input))
              :class    (when (empty? @!query) "invisible")}
             [:i.fa-solid.fa-circle-xmark]]]
           [:div.flex.items-center.text-white
            [:button.mx-2
             {:type "submit"}
             [:i.fa-solid.fa-search]]
            [:a.mx-2.hidden.ml:block
             {:href (rfe/href ::routes/settings)}
             [:i.fa-solid.fa-cog]]]]
          [:div.cursor-pointer.px-2.ml:hidden.text-white
           {:on-click #(rf/dispatch [::events/toggle-mobile-nav])}
           [:i.fa-solid.fa-bars]]
          [mobile-nav show-mobile-nav? service-id service-color services available-kiosks]
          [:div.hidden.ml:flex.w-full
           [:div.relative.flex.flex-col.items-center.justify-center.ml:flex-row
            [:div.w-full.box-border.z-10.ml:text-white
             [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.font-nunito.px-5.w-full
              {:on-change #(rf/dispatch [::events/change-service-kiosk (js/parseInt (.. % -target -value))])
               :value     service-id
               :style     {:background "transparent"}}
              (when services
                (for [service services]
                  [:option.text-white.bg-neutral-900.border-none
                   {:value (:id service) :key (:id service)}
                   (-> service :info :name)]))]]
            [:div.flex.absolute.min-h-full.top-0.right-4.ml:right-0.items-center.justify-end.z-0.ml:text-white
             [:i.fa-solid.fa-caret-down]]]
           [:div.relative.flex-auto.ml:pl-4
            [:ul.flex.font-roboto.flex-col.ml:flex-row.ml:text-white
             (for [kiosk available-kiosks]
               [:li.px-3.py-2 {:key kiosk}
                [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                         :kioskId   kiosk})}
                 kiosk]])]]]]]))))

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
      [:div.flex.flex-col.justify-between.relative.font-nunito {:style {:minHeight "100vh"}}
       (when-let [view (-> current-match :data :view)]
         [view current-match])
       [footer]
       [queue/queue]
       [player/player]]]]))
