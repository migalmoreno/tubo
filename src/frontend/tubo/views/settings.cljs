(ns tubo.views.settings
  (:require
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn boolean-input
  [label key value]
  [:div.w-full.flex.justify-between.items-center.py-2
   [:label label]
   [:input
    {:type      "checkbox"
     :checked   value
     :value     value
     :on-change #(rf/dispatch [::events/change-setting key (not value)])}]])

(defn select-input
  [label key value options]
  [:div.w-full.flex.justify-between.items-center.py-2
   [:label label]
   [:select.focus:ring-transparent.bg-transparent.font-bold.font-nunito
    {:value     value
     :on-change #(rf/dispatch [::events/change-setting key (.. % -target -value)])}
    (for [[i option] (map-indexed vector options)]
      [:option.dark:bg-neutral-900.border-none {:value option :key i} option])]])

(defn settings-page []
  (let [{:keys [current-theme themes show-comments show-related
                show-description]} @(rf/subscribe [:settings])
        service-color              @(rf/subscribe [:service-color])]
    [layout/content-container
     [:h1.text-2xl.font-bold.py-6 "Settings"]
     [:form.flex.flex-wrap
      [select-input "Theme" :current-theme current-theme themes]
      [boolean-input "Show description?" :show-description show-description]
      [boolean-input "Show comments?" :show-comments show-comments]
      [boolean-input "Show related videos?" :show-related show-related]]]))
