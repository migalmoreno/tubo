(ns tubo.channel.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.bookmarks.modals :as modals]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]))

(defn channel
  [query-params]
  (let [!menu-active? (r/atom nil)
        !show-description? (r/atom false)]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [banner avatar name description subscriber-count next-page
                    related-streams]} @(rf/subscribe [:channel])
            next-page-url             (:url next-page)
            service-color             @(rf/subscribe [:service-color])
            scrolled-to-bottom?       @(rf/subscribe [:scrolled-to-bottom])
            page-loading?             @(rf/subscribe [:show-page-loading])]
        (when scrolled-to-bottom?
          (rf/dispatch [:channel/fetch-paginated url next-page-url]))
        [:<>
         (when-not page-loading?
           (when banner
             [:div.flex.justify-center.h-24
              [:img.min-w-full.min-h-full.object-cover {:src banner}]]))
         [layout/content-container
          [:div.flex.items-center.justify-between
           [:div.flex.items-center.my-4.mx-2
            [layout/uploader-avatar {:uploader-avatar avatar :uploader-name name}]
            [:div.m-4
             [:h1.text-2xl.line-clamp-1.font-semibold {:title name} name]
             (when subscriber-count
               [:div.flex.my-2.items-center
                [:i.fa-solid.fa-users.text-xs]
                [:span.mx-2 (.toLocaleString subscriber-count)]])]]
           (when related-streams
             [layout/popover-menu !menu-active?
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [:queue/add-n related-streams])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [:modals/open [modals/add-to-bookmark related-streams]])}]])]
          [layout/show-more-container @!show-description? description
           #(reset! !show-description? (not @!show-description?))]
          [items/related-streams related-streams next-page-url]]]))))
