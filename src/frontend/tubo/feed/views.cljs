(ns tubo.feed.views
  (:require
   [re-frame.core :as rf]
   [tubo.bookmarks.modals :as modals]
   [tubo.ui :as ui]
   [tubo.utils :as utils]))

(defn feed-page
  []
  (let [{:keys [items] :as feed} @(rf/subscribe [:feed])
        last-updated             @(rf/subscribe [:feed-last-updated])]
    [ui/content-container
     [:div.flex.flex-col
      [:div.flex
       [ui/content-header "Feed"]
       [ui/popover
        [{:label    "Add to queue"
          :icon     [:i.fa-solid.fa-headphones]
          :on-click #(rf/dispatch [:queue/add-n items true])}
         {:label    "Add to playlist"
          :icon     [:i.fa-solid.fa-plus]
          :on-click #(rf/dispatch [:modals/open
                                   [modals/add-to-bookmark items]])}]]]
      (when (seq feed)
        [:div.flex.items-center.justify-between.gap-x-6.flex-wrap.gap-y-2
         (when last-updated
           [:span.text-neutral-600.dark:text-neutral-400
            (str "Last updated: " (utils/format-date-ago last-updated))])
         [:div.flex.items-center.gap-x-6.justify-between.flex-auto
          [ui/primary-button "Refresh" #(rf/dispatch [:feed/fetch])
           [:i.fa-solid.fa-refresh]]]])]
     [ui/related-items items nil (:items-layout @(rf/subscribe [:settings]))]]))
