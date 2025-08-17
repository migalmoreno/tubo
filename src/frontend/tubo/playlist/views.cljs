(ns tubo.playlist.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn metadata-popover
  [{:keys [related-streams]} edit-modal]
  (when related-streams
    [layout/popover
     [(when edit-modal
        {:label    "Edit playlist"
         :icon     [:i.fa-solid.fa-pencil]
         :on-click #(rf/dispatch [:modals/open edit-modal])})
      {:label    "Add to queue"
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
    (fn [{:keys [name uploader-name uploader-url stream-count related-streams
                 next-page]
          :as   playlist} url edit-modal]
      [layout/content-container
       [:div.flex.w-full.items-end.py-4.flex-wrap.xs:flex-nowrap.gap-8
        [:div.flex.items-center.justify-center.xs:justify-start.w-full.xs:w-auto
         [layout/thumbnail playlist nil :classes
          ["h-52" "w-52"] :rounded? true]]
        [:div.flex.flex-col.flex-1.gap-y-1
         [:h1.text-sm.font-bold.text-neutral-300
          (str (when edit-modal "LOCAL ") "PLAYLIST")]
         [:div.flex.flex-col.gap-y-6
          [layout/content-header name
           [:div.hidden.xs:block
            [metadata-popover playlist edit-modal]]]
          [:div.flex.items-center.justify-between.gap-x-4
           [:div.flex.gap-x-3.items-center
            (when (seq uploader-name)
              [:<>
               [layout/uploader-avatar playlist :classes ["w-6" "h-6"]]
               [:a.line-clamp-1.font-bold.text-sm
                {:href  (when (seq uploader-url)
                          (rfe/href :channel-page nil {:url uploader-url}))
                 :title uploader-name}
                uploader-name]])
            (when (and (seq uploader-name) (> stream-count 0))
              [layout/bullet])
            (when (> stream-count 0)
              [:span.text-sm.font-semibold.whitespace-nowrap.text-neutral-600.dark:text-neutral-400.flex-auto
               (str stream-count
                    (if (= stream-count 1) " stream" " streams"))])]
           [items/layout-switcher !layout]]]]]
       [items/related-streams related-streams next-page !layout
        #(rf/dispatch [:playlist/fetch-paginated url next-page])]])))

(defn playlist-page
  [{{:keys [url]} :query-params}]
  (let [list @(rf/subscribe [:playlist])]
    [playlist list url]))
