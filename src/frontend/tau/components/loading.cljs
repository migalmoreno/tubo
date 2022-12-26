(ns tau.components.loading
  (:require
   [re-frame.core :as rf]))

(defn page-loading-icon
  [service-color]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fas.fa-circle-notch.fa-spin.text-8xl
    {:style {:color service-color}}]])

(defn pagination-loading-icon
  [service-color loading?]
  [:div.w-full.flex.items-center.justify-center.py-4
   {:class (when-not loading? "invisible")}
   [:i.fas.fa-circle-notch.fa-spin.text-2xl
    {:style {:color service-color}}]])
