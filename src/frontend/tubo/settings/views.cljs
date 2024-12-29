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
  [layout/select-field label value options
   (or on-change
       #(rf/dispatch [:settings/change keys (.. % -target -value)]))])

(defn appearance-settings
  [{:keys [theme items-layout]}]
  [:<>
   [select-input "Theme" [:theme] theme #{:auto :light :dark}]
   [select-input "List view mode" [:items-layout] items-layout #{:grid :list}]])

(defn content-settings
  [{:keys [default-service default-country default-kiosk default-filter
           show-description show-comments show-related]}]
  (let [services   @(rf/subscribe [:services])
        service    @(rf/subscribe [:services/current])
        kiosks     @(rf/subscribe [:kiosks])
        service-id @(rf/subscribe [:service-id])
        countries  (:supported-countries service)]
    [:<>
     [select-input "Default country" nil
      (or (:name (get default-country service-id)) (first countries))
      (map :name countries)
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
     [select-input "Default filter" [:default-filter service-id]
      (or (get default-filter service-id) (first (:content-filters service)))
      (:content-filters service)]
     [boolean-input "Show comments" [:show-comments] show-comments]
     [boolean-input "Show description" [:show-description] show-description]
     [boolean-input "Show 'Next' and 'Similar' videos" [:show-related]
      show-related]]))

(defn video-audio-settings
  [{:keys [default-audio-format default-video-format default-resolution]}]
  [:<>
   [select-input "Default resolution" [:default-resolution]
    default-resolution ["Best" "1080p" "720p" "480p" "360p" "240p" "144p"]]
   [select-input "Default video format" [:default-video-format]
    default-video-format #{"MPEG-4" "WebM" "3GP"}]
   [select-input "Default audio format" [:default-audio-format]
    default-audio-format #{"m4a" "WebM"}]])

(defn settings
  []
  (let [!active-tab (r/atom :video-audio)]
    (fn []
      (let [settings @(rf/subscribe [:settings])]
        [layout/content-container
         [layout/content-header "Settings"]
         [:div.mt-4
          [layout/tabs
           [{:id        :video-audio
             :label     "Video and audio"
             :left-icon [:i.fa-solid.fa-headphones]}
            {:id        :appearance
             :label     "Appearance"
             :left-icon [:i.fa-solid.fa-palette]}
            {:id        :content
             :label     "Content"
             :left-icon [:i.fa-solid.fa-globe]}]
           :selected-id @!active-tab
           :on-change #(reset! !active-tab %)]
          [:form.flex.flex-wrap.py-4.gap-y-4
           (case @!active-tab
             :appearance  [appearance-settings settings]
             :content     [content-settings settings]
             :video-audio [video-audio-settings settings]
             [video-audio-settings settings])]]]))))
