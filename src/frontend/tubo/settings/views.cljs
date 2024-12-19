(ns tubo.settings.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.layout.views :as layout]))

(defn boolean-input
  [label keys value]
  [layout/boolean-input label value
   #(rf/dispatch [:settings/change keys (not value)])])

(defn select-input
  [label keys value options on-change]
  [layout/select-input label value options
   (or on-change
       #(rf/dispatch [:settings/change keys (.. % -target -value)]))])

(defn general-settings
  [{:keys [theme default-service items-layout]}]
  (let [services @(rf/subscribe [:services])]
    [:<>
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
     [select-input "Items Layout" :items-layout items-layout #{:grid :list}]]))

(defn stream-settings
  [{:keys [show-description show-comments show-related]}]
  [:<>
   [boolean-input "Show description" :show-description show-description]
   [boolean-input "Show comments" :show-comments show-comments]
   [boolean-input "Show related videos" :show-related show-related]])

(defn settings
  []
  (let [!active-tab (r/atom :general)]
    (fn []
      (let [settings @(rf/subscribe [:settings])]
        [layout/content-container
         [layout/content-header "Settings"]
         [layout/tabs
          [{:id    :general
            :label "General"}
           {:id    :stream
            :label "Stream"}]
          :selected-id @!active-tab
          :on-change #(reset! !active-tab %)]
         [:form.flex.flex-wrap.py-4
          (case @!active-tab
            :general [general-settings settings]
            :stream  [stream-settings settings]
            [general-settings settings])]]))))
