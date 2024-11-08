(ns tubo.settings.views
  (:require
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]))

(defn boolean-input
  [label key value]
  [layout/boolean-input label key value
   #(rf/dispatch [:settings/change key (not value)])])

(defn select-input
  [label key value options on-change]
  [layout/select-input label key value options
   (or on-change
       #(rf/dispatch [:settings/change key (.. % -target -value)]))])

(defn settings
  []
  (let [{:keys [theme show-comments show-related show-description
                default-service]}
        @(rf/subscribe [:settings])
        services @(rf/subscribe [:services])]
    [layout/content-container
     [layout/content-header "Settings"]
     [:form.flex.flex-wrap.py-4
      [select-input "Theme" :theme theme #{:auto :light :dark}]
      [select-input "Default service" :default-service (:id default-service)
       (map #(-> %
                 :info
                 :name)
            services)
       #(rf/dispatch [:settings/change-service (.. % -target -value)])]
      [select-input "Default kiosk" :default-service
       (:default-kiosk default-service) (:available-kiosks default-service)
       #(rf/dispatch [:settings/change-kiosk (.. % -target -value)])]
      [boolean-input "Show description" :show-description show-description]
      [boolean-input "Show comments" :show-comments show-comments]
      [boolean-input "Show related videos" :show-related show-related]]]))
