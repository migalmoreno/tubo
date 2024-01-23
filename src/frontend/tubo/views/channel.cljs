(ns tubo.views.channel
  (:require
   [re-frame.core :as rf]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.events :as events]))

(defn channel
  [{{:keys [url]} :query-params}]
  (let [{:keys [banner avatar name description subscriber-count
                related-streams next-page]} @(rf/subscribe [:channel])
        next-page-url (:url next-page)
        service-color @(rf/subscribe [:service-color])
        scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [::events/channel-pagination url next-page-url]))
    [layout/content-container
     (when banner
       [:div.flex.justify-center
        [:img.min-w-full {:src banner}]])
     [:div.flex.items-center.justify-between
      [:div.flex.items-center.my-4.mx-2
       [layout/uploader-avatar avatar name]
       [:div.m-4
        [:h1.text-xl name]
        (when subscriber-count
          [:div.flex.my-2.items-center
           [:i.fa-solid.fa-users.text-xs]
           [:span.mx-2 (.toLocaleString subscriber-count)]])]]
      [layout/primary-button "Enqueue"
       #(rf/dispatch [::events/enqueue-related-streams related-streams service-color])
       "fa-solid fa-headphones"]]
     [:div.my-2
      [:p description]]
     [items/related-streams related-streams next-page-url]]))
