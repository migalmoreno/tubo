(ns tubo.playlist.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn metadata-popover
  [_]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [related-streams]}]
      (when related-streams
        [layout/popover-menu !menu-active?
         [{:label    "Add to queue"
           :icon     [:i.fa-solid.fa-headphones]
           :on-click #(rf/dispatch [:queue/add-n related-streams true])}
          {:label    "Add to playlist"
           :icon     [:i.fa-solid.fa-plus]
           :on-click #(rf/dispatch [:modals/open
                                    [modals/add-to-bookmark
                                     related-streams]])}]]))))

(defn playlist
  [_]
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [name next-page uploader-name uploader-url related-streams
                    stream-count]
             :as   playlist}
            @(rf/subscribe [:playlist])
            next-page-url (:url next-page)
            scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])]
        (when scrolled-to-bottom?
          (rf/dispatch [:playlist/fetch-paginated url next-page-url]))
        [layout/content-container
         [:div.flex.flex-col.justify-center
          [layout/content-header
           [:div.flex.flex-col.gap-y-2.mb-2 name
            [:span.text-sm.whitespace-nowrap.text-neutral-600.dark:text-neutral-400
             (str stream-count " streams")]]
           [:div.hidden.lg:block
            [metadata-popover playlist]]]
          [:div.flex.items-center.justify-between.my-4.gap-x-4
           [:div.flex.items-center
            [layout/uploader-avatar playlist]
            [:a.line-clamp-1.ml-2
             {:href  (rfe/href :channel-page nil {:url uploader-url})
              :title uploader-name}
             uploader-name]]]]
         [items/layout-switcher !layout]
         [items/related-streams related-streams next-page-url !layout]]))))
