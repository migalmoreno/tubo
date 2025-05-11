(ns tubo.playlist.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn metadata-popover
  [{:keys [related-streams]}]
  (when related-streams
    [layout/popover
     [{:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:queue/add-n related-streams true])}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [modals/add-to-bookmark related-streams]])}]
     :extra-classes ["px-5" "xs:p-3"]
     :tooltip-classes ["right-7" "top-0"]]))

(defn playlist
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [name next-page uploader-name uploader-url related-streams
                    stream-count]
             :as   playlist}
            @(rf/subscribe [:playlist])
            next-page-url (:url next-page)]
        [layout/content-container
         [:div.flex.w-full.flex-auto.items-start.py-4.flex-wrap.md:flex-nowrap.gap-8
          [:div.flex.items-center.justify-center.md:justify-start.flex-auto.lg:flex-none
           [layout/thumbnail playlist nil :classes
            ["h-52" "w-52"] :rounded? true]]
          [:div.flex.flex-col.flex-auto
           [layout/content-header name
            [:div.hidden.xs:block
             [metadata-popover playlist]]]
           [:span.text-sm.font-semibold.whitespace-nowrap.text-neutral-600.dark:text-neutral-400.flex-auto
            (str stream-count " streams")]
           [:div.flex.items-center.justify-between.my-4.gap-x-4
            [:div.flex.gap-x-3.items-center
             [layout/uploader-avatar playlist :classes ["w-6" "h-6"]]
             [:a.line-clamp-1
              {:href  (rfe/href :channel-page nil {:url uploader-url})
               :title uploader-name}
              uploader-name]]
            [items/layout-switcher !layout]]]]
         [items/related-streams related-streams next-page-url !layout
          #(rf/dispatch [:playlist/fetch-paginated url next-page-url])]]))))
