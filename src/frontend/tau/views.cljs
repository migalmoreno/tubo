(ns tau.views
  (:require
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]
   [reagent.ratom :as ratom]
   [tau.components.navigation :as navigation]
   [tau.components.player :as player]
   [tau.events :as events]
   [tau.routes :as routes]))

(defonce scroll-hook (.addEventListener js/window "scroll" #(rf/dispatch [::events/page-scroll])))
(defonce services (rf/dispatch [::events/get-services]))
(defonce kiosks (rf/dispatch [::events/get-kiosks 0]))

(defn footer
  []
  [:footer
   [:div.bg-black.text-gray-300.p-5.text-center.w-full
    [:div.flex.flex-col.justify-center
     [:div
      [:p.px-2 (str "Tau " (.getFullYear (js/Date.)))]]
     [:div.pt-4
      [:a {:href "https://sr.ht/~conses/tau"}
       [:i.fa-solid.fa-code]]]]]])

(defn navbar
  [{{:keys [serviceId]} :query-params}]
  (let [service-id @(rf/subscribe [:service-id])
        service-color @(rf/subscribe [:service-color])
        global-search @(rf/subscribe [:global-search])
        services @(rf/subscribe [:services])
        id (js/parseInt (or serviceId service-id))
        {:keys [available-kiosks default-kiosk]} @(rf/subscribe [:kiosks])]
    [:nav.flex.p-2.content-center.sticky.top-0.z-50.font-nunito
     {:style {:background service-color}}
     [:div.flex
      [:form.flex.items-center
       {:on-submit (fn [e]
                     (.preventDefault e)
                     (rf/dispatch [::events/navigate
                                   {:name ::routes/search
                                    :params {}
                                    :query  {:q global-search :serviceId service-id}}]))}
       [:div
        [:a.px-5.text-white.font-bold.font-nunito
         {:href (rfe/href ::routes/home) :dangerouslySetInnerHTML {:__html "&tau;"}}]]
       [:div.relative
        [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.font-nunito
         {:on-change #(rf/dispatch [::events/change-service (js/parseInt (.. % -target -value))])
          :value service-id
          :style {:background "transparent"}}
         (when services
           (for [service services]
             [:option.bg-neutral-900.border-none {:value (:id service) :key (:id service)}
              (-> service :info :name)]))]
        [:div.flex.absolute.min-h-full.min-w-full.top-0.right-0.items-center.justify-end
         {:style {:zIndex "-1"}}
         [:i.fa-solid.fa-caret-down.mr-4]]]
       [:div
        [:input.bg-transparent.border-none.rounded.py-2.px-1.mx-2.focus:ring-transparent.placeholder-white
         {:type "text"
          :value global-search
          :on-change #(rf/dispatch [::events/change-global-search (.. % -target -value)])
          :placeholder "Search for something"}]]
       [:div
        [:button.text-white.mx-2
         {:type "submit"}
         [:i.fas.fa-search]]]]
      [:div
       [:ul.flex.content-center.p-2.text-white.font-roboto
        (for [kiosk available-kiosks]
          [:li.px-5 {:key kiosk}
           [:a {:href (rfe/href ::routes/kiosk nil {:serviceId service-id
                                                    :kioskId kiosk})}
            kiosk]])]]]]))

(defn app
  []
  (let [current-match @(rf/subscribe [:current-match])]
    [:div.min-h-screen.flex.flex-col.h-full.text-white.bg-neutral-900
     [navbar current-match]
     [:div.flex.flex-col.justify-between.relative.font-nunito {:class "min-h-[calc(100vh-58px)]"}
      (when-let [view (-> current-match :data :view)]
        [view current-match])
      [footer]
      [player/global-player]]]))
