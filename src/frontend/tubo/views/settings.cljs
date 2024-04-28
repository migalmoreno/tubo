(ns tubo.views.settings
  (:require
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn boolean-input
  [label key value]
  [layout/boolean-input label key value #(rf/dispatch [::events/change-setting key (not value)])])

(defn select-input
  [label key value options on-change]
  [layout/select-input label key value options
   (or on-change #(rf/dispatch [::events/change-setting key (.. % -target -value)]))])

(defn settings-page []
  (let [{:keys [theme themes show-comments show-related
                show-description default-service]} @(rf/subscribe [:settings])
        service-color                              @(rf/subscribe [:service-color])
        services                                   @(rf/subscribe [:services])]
    [layout/content-container
     [layout/content-header "Settings"]
     [:form.flex.flex-wrap.py-4
      [select-input "Theme" :theme theme #{:auto :light :dark}]
      [select-input "Default service" :default-service (:id default-service)
       (map #(-> % :info :name) services)
       #(rf/dispatch [::events/change-service-setting (..  % -target -value)])]
      [select-input "Default kiosk" :default-service
       (:default-kiosk default-service) (:available-kiosks default-service)
       #(rf/dispatch [::events/change-kiosk-setting (..  % -target -value)])]
      [boolean-input "Show description?" :show-description show-description]
      [boolean-input "Show comments?" :show-comments show-comments]
      [boolean-input "Show related videos?" :show-related show-related]]]))
