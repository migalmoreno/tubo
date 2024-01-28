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
  [label key value options on-change]
  [:div.w-full.flex.justify-between.items-center.py-2
   [:label label]
   [:select.focus:ring-transparent.bg-transparent.font-bold.font-nunito
    {:value     value
     :on-change (or on-change #(rf/dispatch [::events/change-setting key (.. % -target -value)]))}
    (for [[i option] (map-indexed vector options)]
      [:option.dark:bg-neutral-900.border-none {:value option :key i} option])]])

(defn settings-page []
  (let [{:keys [theme themes show-comments show-related
                show-description default-service]} @(rf/subscribe [:settings])
        service-color                              @(rf/subscribe [:service-color])
        services                                   @(rf/subscribe [:services])]
    [layout/content-container
     [layout/content-header "Settings"]
     [:form.flex.flex-wrap.py-4
      [select-input "Theme" :theme theme #{:light :dark}]
      [select-input "Default service" :default-service (:id default-service)
       (map #(-> % :info :name) services)
       #(rf/dispatch [::events/change-service-setting (..  % -target -value)])]
      [select-input "Default kiosk" :default-service
       (:default-kiosk default-service) (:available-kiosks default-service)
       #(rf/dispatch [::events/change-kiosk-setting (..  % -target -value)])]
      [boolean-input "Show description?" :show-description show-description]
      [boolean-input "Show comments?" :show-comments show-comments]
      [boolean-input "Show related videos?" :show-related show-related]]]))
