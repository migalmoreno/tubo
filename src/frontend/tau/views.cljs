(ns tau.views
  (:require
   [tau.views.player :as player]
   [reitit.frontend.easy :as rfe]
   [re-frame.core :as rf]))

(defn footer
  []
  [:footer.bg-slate-900.text-gray-300.p-5.text-center
   [:div
    [:p (str "Tau " (.getFullYear (js/Date.)))]]])

(defn search-bar
  []
  (let [global-search @(rf/subscribe [:global-search])
        services @(rf/subscribe [:services])
        service-id @(rf/subscribe [:service-id])]
    [:div.flex
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (rfe/push-state :tau.routes/search {} {:q global-search :serviceId service-id}))}
      [:input.bg-slate-900.border.border-solid.border-black.rounded.py-2.px-1.mx-2.text-gray-500
       {:type "text"
        :value global-search
        :on-change #(rf/dispatch [:change-global-search (.. % -target -value)])
        :placeholder "Search for something"}]
      [:select.mx-2.bg-gray-50.border.border-gray-900.text-gray-900
       {:on-change #(rf/dispatch [:change-service-id (js/parseInt (.. % -target -value))])}
       (for [service services]
         [:option {:value (:id service) :key (:id service) :selected (= (:id service) service-id)}
          (-> service :info :name)])]
      [:button..bg-slate-900.border.border-black.rounded.border-solid.text-gray-500.p-2.mx-2
       {:type "submit"} "Search"]]]))

(defn navbar []
  [:nav.bg-slate-800.flex.p-2.content-center.sticky.top-0.z-50
   [:div.px-5.text-white.p-2
    [:a {:href (rfe/href :tau.routes/home) :dangerouslySetInnerHTML {:__html "&tau;"}}]]
   [:ul.flex.content-center.text-white.p-2
    [:li.px-5 [:a {:href (rfe/href :tau.routes/home)} "Home"]]
    [:li.px-5 [:a {:href (rfe/href :tau.routes/search)} "Search"]]]
   [search-bar]])

(defn app
  []
  (rf/dispatch [:get-services])
  (let [current-match @(rf/subscribe [:current-match])]
    [:div.font-sans.bg-slate-700.min-h-screen.flex.flex-col.h-full
     [navbar]
     [:div.flex.flex-col.justify-between {:class "min-h-[calc(100vh-58px)]"}
      (when-let [view (-> current-match :data :view)]
        [view current-match])
      [player/global-player]
      [footer]]]))
