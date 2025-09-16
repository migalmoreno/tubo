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
  (let [service-color @(rf/subscribe [:service-color])]
    [layout/form-field {:label label :orientation :vertical}
     [layout/input
      :type :checkbox
      :class ["rounded"]
      :style {:color service-color}
      :checked value
      :value value
      :on-change
      #(rf/dispatch [:settings/change keys (.. % -target -checked)])]]))

(defn text-input
  [label keys value]
  [layout/form-field {:label label :orientation :vertical}
   [layout/input
    :value value
    :on-change #(rf/dispatch [:settings/change keys (.. % -target -value)])]])

(defn select-input
  [label keys value options on-change]
  [layout/form-field {:label label :orientation :vertical}
   [layout/select value options
    (or on-change
        #(rf/dispatch [:settings/change keys (.. % -target -value)]))]])

(defn generic-input
  [label children]
  [layout/form-field {:label label :orientation :vertical} children])

(defn appearance-settings
  [{:keys [theme items-layout]}]
  [:<>
   [select-input "Theme" [:theme] theme #{:auto :light :dark}]
   [select-input "List view mode" [:items-layout] items-layout #{:grid :list}]])

(defn add-peertube-instance
  []
  [modals/modal-content "Create New PeerTube Instance?"
   [layout/form
    {:validation  [:map
                   [:url
                    [:fn
                     {:error/fn (constantly "should be a URL")}
                     (fn [value]
                       (if (seq value) (.canParse js/URL value) true))]]]
     :on-submit   [:peertube/create-instance]
     :submit-text "Create"}
    [{:name        :url
      :label       "Instance URL"
      :placeholder "URL"
      :type        :text}]]])

(defn peertube-instances-modal
  []
  (let [instances @(rf/subscribe [:peertube/instances])]
    [modals/modal-content "PeerTube instances"
     [:fieldset.flex.gap-y-2.flex-col
      (for [[i instance] (map-indexed vector instances)]
        ^{:key i}
        [:div.bg-neutral-200.dark:bg-neutral-900.flex.rounded.py-2.px-4.justify-between.flex.items-center
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
           {:style           {:color (utils/get-service-color 3)}
            :type            "radio"
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
      (map (fn [kiosk] {:label (utils/titleize kiosk) :value kiosk})
           (:available-kiosks kiosks))]
     [select-input "Default filter" [:default-filter service-id]
      (or (get default-filter service-id) (first (:content-filters service)))
      (map (fn [filter] {:label (utils/titleize filter) :value filter})
           (:content-filters service))]
     [text-input "API instance" [:instance] instance]
     [text-input "Authentication instance" [:auth-instance] auth-instance]
     [generic-input "PeerTube instances"
      [layout/primary-button "Edit"
       #(rf/dispatch [:modals/open [peertube-instances-modal]])]]
     [select-input "Image quality" [:image-quality]
      image-quality
      (map (fn [[value label]] {:label label :value value})
           (into [] qualities))]
     [boolean-input "Show comments" [:show-comments] show-comments]
     [boolean-input "Show description" [:show-description] show-description]
     [boolean-input "Show related streams" [:show-related] show-related]]))

(defn user-settings
  []
  [:<>
   [generic-input "Logout"
    [:div.flex.gap-x-4
     [layout/primary-button "This device" #(rf/dispatch [:auth/logout])]
     [layout/secondary-button "All devices"
      #(rf/dispatch [:auth/invalidate-session])]]]
   [generic-input "Password Reset"
    [layout/primary-button "Reset"
     #(rf/dispatch [:modals/open [auth/password-reset-modal]])]]
   [generic-input "Delete User"
    [layout/primary-button "Delete"
     #(rf/dispatch [:modals/open [auth/user-deletion-modal]])]]])

(defn video-audio-settings
  [{:keys [default-audio-format default-video-format default-resolution
           seamless-playback]}]
  [:<>
   [select-input "Default resolution" [:default-resolution]
    default-resolution ["Best" "1080p" "720p" "480p" "360p" "240p" "144p"]]
   [select-input "Default video format" [:default-video-format]
    default-video-format #{"MPEG-4" "WebM" "3GP"}]
   [select-input "Default audio format" [:default-audio-format]
    default-audio-format #{"m4a" "WebM"}]
   [boolean-input "Seamless playback" [:seamless-playback] seamless-playback]])

(defn settings
  []
  (let [user        @(rf/subscribe [:auth/user])
        breakpoint  @(rf/subscribe [:layout/breakpoint])
        !active-tab (r/atom (cond (and user (not= breakpoint :sm)) :user
                                  (not= breakpoint :sm)            :video-audio
                                  :else                            nil))]
    (fn []
      (let [settings @(rf/subscribe [:settings])]
        [layout/content-container
         [layout/content-header "Settings"]
         [:div.mt-4.lg:mt-8.flex.gap-y-4.gap-x-12.md:flex-nowrap.flex-wrap
          [layout/horizontal-tabs
           [(when user
              {:id        :user
               :label     "User"
               :left-icon [:i.fa-solid.fa-user]})
            {:id        :video-audio
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
          [:form.flex.flex-wrap.gap-y-4.w-full.h-fit.items-start
           {:on-submit #(.preventDefault %)}
           (case @!active-tab
             :appearance  [appearance-settings settings]
             :content     [content-settings settings]
             :user        [user-settings settings]
             :video-audio [video-audio-settings settings]
             nil)]]]))))
