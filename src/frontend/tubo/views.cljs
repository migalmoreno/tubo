(ns tubo.views
  (:require
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reagent.ratom :as ratom]
   [tubo.components.navigation :as navigation]
   [tubo.components.player :as player]
   [tubo.events :as events]
   [tubo.routes :as routes]))

(defonce scroll-hook (.addEventListener js/window "scroll" #(rf/dispatch [::events/page-scroll])))
(defonce mobile-touch-hook (.addEventListener js/document.body "touchmove" #(rf/dispatch [::events/page-scroll])))
(defonce services (rf/dispatch [::events/get-services]))
(defonce kiosks (rf/dispatch [::events/get-kiosks 0]))

(defn navbar
  [{{:keys [serviceId]} :query-params}]
  (let [service-id @(rf/subscribe [:service-id])
        service-color @(rf/subscribe [:service-color])
        global-search @(rf/subscribe [:global-search])
        services @(rf/subscribe [:services])
        id (js/parseInt (or serviceId service-id))
        mobile-nav? @(rf/subscribe [:show-mobile-nav])
        {:keys [available-kiosks default-kiosk]} @(rf/subscribe [:kiosks])]
    [:nav.flex.px-2.py-2.5.items-center.sticky.top-0.z-50.font-nunito
     {:style {:background service-color}}
     [:div.flex.items-center.justify-between.flex-auto
      [:div.py-2
       [:a.px-5.text-white.font-bold
        {:href (rfe/href ::routes/home)}
        "tubo"]]
      [:form.flex.items-center.relative
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::events/navigate
                                   {:name ::routes/search
                                    :params {}
                                    :query  {:q global-search :serviceId service-id}}]))}
       [:div
        [:input.bg-transparent.border-none.rounded.py-2.px-1.focus:ring-transparent.placeholder-white.box-border.w-40.xs:w-auto
         {:type "text"
          :value global-search
          :on-change #(rf/dispatch [::events/change-global-search (.. % -target -value)])
          :placeholder "Search for something"}]]
       [:div.flex.items-center.px-2
        [:button.text-white
         {:type "submit"}
         [:i.fas.fa-search]]]]
      [:div.cursor-pointer.px-2.ml:hidden
       {:on-click #(rf/dispatch [::events/toggle-mobile-nav])}
       [:i.fa-solid.fa-bars]]
      [:div.bg-neutral-900.items-center.fixed.overflow-x-hidden.min-h-screen.w-60.top-0.shadow-xl.shadow-black.pt-8
       {:class (str "ease-in-out delay-75 transition-[right] "
                    "ml:w-full ml:flex ml:min-h-0 ml:relative ml:text-white ml:bg-transparent ml:shadow-none ml:p-0 ml:right-0 "
                    (if mobile-nav? "right-0" "right-[-245px]"))}
       [:div.cursor-pointer.px-2.ml:hidden.absolute.top-1.right-2
        {:on-click #(rf/dispatch [::events/toggle-mobile-nav])}
        [:i.fa-solid.fa-close.text-xl]]
       [:div.relative.flex.flex-col.items-center.justify-center.ml:flex-row
        [:div.w-full.box-border.z-10
         [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.font-nunito.px-5.w-full
          {:on-change #(rf/dispatch [::events/change-service (js/parseInt (.. % -target -value))])
           :value service-id
           :style {:background "transparent"}}
          (when services
            (for [service services]
              [:option.bg-neutral-900.border-none {:value (:id service) :key (:id service)}
               (-> service :info :name)]))]]
        [:div.flex.absolute.min-h-full.top-0.right-4.ml:right-0.items-center.justify-end.z-0
         [:i.fa-solid.fa-caret-down]]]
       [:div.relative.flex-auto.ml:pl-4
        [:ul.flex.font-roboto.flex-col.ml:flex-row
         (for [kiosk available-kiosks]
           [:li.px-5.py-2 {:key kiosk}
            [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                     :kioskId kiosk})}
             kiosk]])]]]]]))

(defn footer
  []
  [:footer
   [:div.bg-black.text-gray-300.p-5.text-center.w-full
    [:div.flex.flex-col.justify-center.items-center
     [:div.flex.items-center.justify-center
      [:div.items-center
       [:a.font-bold {:href "https://git.mianmoreno.com/tubo.git"} "tubo"]]
      [:div
       [:p.px-2 (str "2022-" (.getFullYear (js/Date.)))]]]]]])

(defn app
  []
  (let [current-match @(rf/subscribe [:current-match])]
    [:div.min-h-screen.flex.flex-col.h-full.text-white.bg-neutral-900.relative
     [navbar current-match]
     [:div.flex.flex-col.justify-between.relative.font-nunito {:class "min-h-[calc(100vh-58px)]"}
      (when-let [view (-> current-match :data :view)]
        [view current-match])
      [footer]
      [player/global-player]]]))
