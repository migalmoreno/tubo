(ns tubo.views.settings
  (:require
   [re-frame.core :as rf]
   [tubo.events :as events]
   [tubo.components.navigation :as navigation]))

(defn boolean-input
  [label key value]
  [:div.w-full.flex.justify-between.items-center.py-2
   [:label label]
   [:input
    {:type      "checkbox"
     :checked value
     :value value
     :on-change #(rf/dispatch [::events/change-setting key (not value)])}]])

(defn settings-page []
  (let [{:keys [current-theme themes show-comments show-related
                show-description]} @(rf/subscribe [:settings])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col.items-center.px-5.py-2.flex-auto
     [:div.flex.flex-col.flex-auto {:class "ml:w-4/5 xl:w-3/5"}
      [navigation/back-button service-color]
      [:h1.text-2xl.font-bold.py-6 "Settings"]
      [:form.flex.flex-wrap
       [:div.w-full.flex.justify-between.items-center.py-2
        [:label "Theme"]
        [:select.focus:ring-transparent.bg-transparent.font-bold.font-nunito
         {:value     current-theme
          :on-change #(rf/dispatch [::events/change-setting :current-theme (.. % -target -value)])}
         (for [[i theme] (map-indexed vector themes)]
           [:option.dark:bg-neutral-900.border-none {:value theme :key i} theme])]]
       [boolean-input "Show description?" :show-description show-description]
       [boolean-input "Show comments?" :show-comments show-comments]
       [boolean-input "Show related videos?" :show-related show-related]]]]))
