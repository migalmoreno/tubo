(ns tau.views
  (:require
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reagent.ratom :as ratom]
   [tau.components.navigation :as navigation]
   [tau.events :as events]
   [tau.routes :as routes]
   [tau.views.player :as player]))

(defonce scroll-hook (.addEventListener js/window "scroll" #(rf/dispatch [::events/page-scroll])))
(defonce services (rf/dispatch [::events/get-services]))

(defn footer
  []
  [:footer
   [:div.bg-black.text-gray-300.p-5.text-center.w-full
    [:p (str "Tau " (.getFullYear (js/Date.)))]]])

(defn search-bar
  [{{:keys [serviceId]} :query-params}]
  (let [global-search @(rf/subscribe [:global-search])
        services @(rf/subscribe [:services])
        service-id @(rf/subscribe [:service-id])
        id (js/parseInt (or serviceId service-id)) ]
    [:div.flex
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (rf/dispatch [::events/navigate
                                        {:name ::routes/search
                                         :params {}
                                         :query  {:q global-search :serviceId service-id}}]))}
      [:input.bg-neutral-900.border.border-solid.border-black.rounded.py-2.px-1.mx-2.text-gray-500
       {:type "text"
        :value global-search
        :on-change #(rf/dispatch [::events/change-global-search (.. % -target -value)])
        :placeholder "Search for something"}]
      [:select.mx-2.bg-gray-50.border.border-gray-900.text-gray-900
       {:on-change #(rf/dispatch [::events/change-service-id (js/parseInt (.. % -target -value))])}
       (when services
         (for [service services]
           [:option {:value (:id service) :key (:id service) :selected (= id (:id service))}
            (-> service :info :name)]))]
      [:button.text-white.mx-2
       {:type "submit"}
       [:i.fas.fa-search]]]]))

(defn navbar
  [match]
  (let [service-id @(rf/subscribe [:service-id])
        service-color @(rf/subscribe [:service-color])
        {:keys [default-kiosk available-kiosks]} @(rf/subscribe [:kiosks])]
    (rf/dispatch [::events/get-kiosks service-id])
    [:nav.flex.p-2.content-center.sticky.top-0.z-50
     {:style {:background service-color}}
     [:div.px-5.text-white.p-2.font-bold
      [:a {:href (rfe/href ::routes/home) :dangerouslySetInnerHTML {:__html "&tau;"}}]]
     [:ul.flex.content-center.p-2.text-white
      (for [kiosk available-kiosks]
        [:li.px-5 [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                           :kioskId kiosk})}
                   kiosk]])]
     [search-bar match]]))

(defn app
  []
  (let [current-match @(rf/subscribe [:current-match])]
    [:div.font-sans.min-h-screen.flex.flex-col.h-full {:style {:background "rgba(23, 23, 23)"}}
     [navbar current-match]
     [:div.flex.flex-col.justify-between.relative {:class "min-h-[calc(100vh-58px)]"}
      (when-let [view (-> current-match :data :view)]
        [view current-match])
      [player/global-player]
      [footer]]]))
