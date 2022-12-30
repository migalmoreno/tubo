(ns tau.views.channel
  (:require
   [re-frame.core :as rf]
   [tau.components.items :as items]
   [tau.components.loading :as loading]
   [tau.components.navigation :as navigation]
   [tau.events :as events]))

(defn channel
  [{{:keys [url]} :query-params}]
  (let [{:keys [banner avatar name description subscriber-count
                related-streams next-page]} @(rf/subscribe [:channel])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        page-loading? @(rf/subscribe [:show-page-loading])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        page-scroll @(rf/subscribe [:page-scroll])
        scrolled-to-bottom? (= page-scroll (.-scrollHeight js/document.body))]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/channel-pagination url next-page-url]))
    [:div.flex.flex-col.items-center.px-5.py-2.flex-auto
     (if page-loading?
       [loading/loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto {:class "ml:w-4/5 xl:w-3/5"}
        [navigation/back-button service-color]
        (when banner
          [:div.flex.justify-center
           [:img {:src banner}]])
        [:div.flex.items-center.my-4.mx-2
         (when avatar
           [:div.relative.w-16.h-16
            [:img.rounded-full.object-cover.max-w-full.min-h-full {:src avatar :alt name}]])
         [:div.m-4
          [:h1.text-xl name]
          (when subscriber-count
            [:div.flex.my-2.items-center
             [:i.fa-solid.fa-users.text-xs]
             [:span.mx-2 (.toLocaleString subscriber-count)]])]]
        [:div.my-2
         [:p description]]
        [items/related-streams related-streams next-page-url]])]))
