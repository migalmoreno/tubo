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

(defn appearance-settings
  [{:keys [theme items-layout]}]
  [:<>
   [select-input "Theme" [:theme] theme #{:auto :light :dark}]
   [select-input "List view mode" [:items-layout] items-layout #{:grid :list}]])

(defn content-settings
  [{:keys [default-service default-country default-kiosk show-description
           show-comments show-related]}]
  (let [services   @(rf/subscribe [:services])
        kiosks     @(rf/subscribe [:kiosks])
        service-id @(rf/subscribe [:service-id])
        countries  (->> services
                        (filter #(= (:id %) service-id))
                        first
                        :supported-countries)]
    [:<>
     [select-input "Default country" nil
      (:name (get default-country service-id)) (map :name countries)
      (fn [e]
        (rf/dispatch [:settings/change [:default-country service-id]
                      (first (filter #(= (.. e -target -value) (:name %))
                                     countries))]))]
     [select-input "Default service" nil
      (->> services
           (filter #(= (:id %) default-service))
           first
           :info
           :name)
      (map #(-> %
                :info
                :name)
           services)
      (fn [e]
        (rf/dispatch [:settings/change [:default-service]
                      (->> services
                           (filter #(= (.. e -target -value)
                                       (-> %
                                           :info
                                           :name)))
                           first
                           :id)]))]
     [select-input "Default kiosk" [:default-kiosk service-id]
      (or (get default-kiosk service-id) (:default-kiosk kiosks))
      (:available-kiosks kiosks)]
     [boolean-input "Show comments" [:show-comments] show-comments]
     [boolean-input "Show description" [:show-description] show-description]
     [boolean-input "Show 'Next' and 'Similar' videos" [:show-related]
      show-related]]))

(defn settings
  []
  (let [!active-tab (r/atom :appearance)]
    (fn []
      (let [settings @(rf/subscribe [:settings])]
        [layout/content-container
         [layout/content-header "Settings"]
         [:div.mt-4
          [layout/tabs
           [{:id        :appearance
             :label     "Appearance"
             :left-icon [:i.fa-solid.fa-palette]}
            {:id        :content
             :label     "Content"
             :left-icon [:i.fa-solid.fa-globe]}]
           :selected-id @!active-tab
           :on-change #(reset! !active-tab %)]
          [:form.flex.flex-wrap.py-4.gap-y-4
           (case @!active-tab
             :appearance [appearance-settings settings]
             :content    [content-settings settings]
             [appearance-settings settings])]]]))))
