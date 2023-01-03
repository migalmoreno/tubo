(ns tau.components.player
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.events :as events]))

(defn global-player
  []
  (let [{:keys [uploader-name uploader-url name stream url service-color]} @(rf/subscribe [:global-stream])
        show-global-player? @(rf/subscribe [:show-global-player])]
    (when show-global-player?
      [:div.sticky.bottom-0.z-50.bg-neutral-900.p-5.absolute
       {:style {:borderColor service-color :borderTopWidth "2px" :borderStyle "solid"}}
       [:div.flex.items-center.justify-between
        [:div.flex.flex-wrap.items-center
         [:div.flex.flex-col
          [:a.text-xs
           {:href (rfe/href :tau.router/stream nil {:url url})} name]
          [:a.text-xs.text-gray-300
           {:href (rfe/href :tau.router/channel nil {:url uploader-url})} uploader-name]]
         [:div.px-2.py-0.md:pt-4
          [:audio {:src stream :controls true}]]]
        [:div.px-2
         [:i.fa-solid.fa-close.cursor-pointer {:on-click #(rf/dispatch [::events/toggle-global-player])}]]]])))
