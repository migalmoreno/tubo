(ns tubo.settings.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tubo.layout.views :as layout]
   [tubo.modals.views :as modals]
   [tubo.utils :as utils]
   [tubo.auth.views :as auth]))

(defn boolean-input
  [label keys value]
  [layout/boolean-input label value
   #(rf/dispatch [:settings/change keys (not value)])])

(defn text-input
  [label keys value]
  [layout/form-field {:label label :orientation :vertical}
   [layout/text-input value
    #(rf/dispatch [:settings/change keys (.. % -target -value)])]])

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

(defn add-peertube-instance
  []
  (let [!instance (r/atom {})]
    (fn []
      [modals/modal-content "Create New PeerTube Instance?"
       [layout/text-field "URL" (:url @!instance)
        #(swap! !instance assoc :url (.. % -target -value))
        "Instance URL"]
       [layout/secondary-button "Cancel"
        #(rf/dispatch [:modals/close])]
       [layout/primary-button "Create"
        #(rf/dispatch [:peertube/create-instance @!instance])]])))

(defn peertube-instances-modal
  []
  (let [instances @(rf/subscribe [:peertube/instances])]
    [modals/modal-content "PeerTube instances"
     [:fieldset.flex.gap-y-2.flex-col
      (for [[i instance] (map-indexed vector instances)]
        ^{:key i}
        [:div.bg-neutral-300.dark:bg-neutral-800.flex.rounded.py-2.px-4.justify-between.flex.items-center
         [:div.flex.flex-col
          [:label.text-lg {:for (:name instance)} (:name instance)]
          [:a
           {:href   (:url instance)
            :target "blank"
            :rel    "noopener"
            :style  {:color (utils/get-service-color 3)}}
           (:url instance)]]
         [:div.flex.gap-x-8
          (when-not (:active? instance)
            [:i.fa-solid.fa-trash.cursor-pointer
             {:on-click #(rf/dispatch [:peertube/delete-instance instance])}])
          [:input
           {:type            "radio"
            :id              (:name instance)
            :name            "instance"
            :on-change       #(rf/dispatch [:peertube/change-instance instance])
            :default-checked (:active? instance)
            :default-value   (:url instance)}]]])]
     [layout/secondary-button "Cancel"
      #(rf/dispatch [:modals/close])]
     [layout/primary-button "Add"
      #(rf/dispatch [:modals/open [add-peertube-instance]])]]))

(defn content-settings
  [{:keys [default-service default-country default-kiosk default-filter
           show-description show-comments show-related instance auth-instance
           image-quality]}]
  (let [services   @(rf/subscribe [:services])
        service    @(rf/subscribe [:services/current])
        kiosks     @(rf/subscribe [:kiosks])
        service-id @(rf/subscribe [:service-id])
        countries  (:supported-countries service)
        qualities  {"none"   "Do not load images"
                    "low"    "Low quality"
                    "medium" "Medium quality"
                    "high"   "High quality"}]
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
     [text-input "API instance" [:instance] instance]
     [text-input "Authentication instance" [:auth-instance] auth-instance]
     [layout/generic-input "PeerTube instances"
      [layout/primary-button "Edit"
       #(rf/dispatch [:modals/open [peertube-instances-modal]])]]
     [select-input "Image Quality" [:image-quality]
      image-quality
      (map (fn [[value label]] {:label label :value value})
           (into [] qualities))]
     [boolean-input "Show comments" [:show-comments] show-comments]
     [boolean-input "Show description" [:show-description] show-description]
     [boolean-input "Show 'Next' and 'Similar' videos" [:show-related]
      show-related]]))

(defn user-settings
  []
  [:<>
   [layout/generic-input "Logout"
    [:div.flex.gap-x-4
     [layout/primary-button "This device" #(rf/dispatch [:auth/logout])]
     [layout/secondary-button "All devices"
      #(rf/dispatch [:auth/invalidate-session])]]]
   [layout/generic-input "Password Reset"
    [layout/primary-button "Reset"
     #(rf/dispatch [:modals/open [auth/password-reset-modal]])]]
   [layout/generic-input "Delete User"
    [layout/primary-button "Delete"
     #(rf/dispatch [:modals/open [auth/user-deletion-modal]])]]])

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
  (let [user        @(rf/subscribe [:auth/user])
        !active-tab (r/atom nil)]
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
          [:form.flex.flex-wrap.py-4.gap-y-4 {:on-submit #(.preventDefault %)}
           (case @!active-tab
             :appearance  [appearance-settings settings]
             :content     [content-settings settings]
             :video-audio [video-audio-settings settings])]]]))))
