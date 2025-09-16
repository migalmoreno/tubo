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
     :extra-classes ["px-5" "xs:px-3"]]))

(defn playlist
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn [{:keys [name uploader-name uploader-url stream-count related-streams
                 next-page]
          :as   playlist} url edit-modal]
      (let [dark-theme? @(rf/subscribe [:dark-theme])
            bg-color    (str "rgba("
                             (if dark-theme? "10,10,10" "255,255,255")
                             ","
                             (if dark-theme? 0.5 0.2)
                             ")")
            bg-image    (str "linear-gradient("
                             bg-color
                             ","
                             bg-color
                             "),url("
                             (:thumbnail playlist)
                             ")")]
        [layout/content-container
         [:div.flex.w-full.items-end.py-4.flex-wrap.xs:flex-nowrap.gap-8.relative.before:absolute.before:top-0.before:bottom-0.before:left-0.before:right-0.before:bg-cover.before:bg-center.before:bg-no-repeat.before:h-24.rounded
          {:style {"--bg-image" bg-image}
           :class ["before:bg-[image:var(--bg-image)]"
                   "before:content-['']" "before:scale-y-[3]"
                   "before:scale-x-[1.075]"
                   "before:blur-[50px]"]}
          [:div.flex.items-center.justify-center.xs:justify-start.w-full.xs:w-auto.relative
           [layout/thumbnail playlist nil :container-classes ["h-52" "w-52"]
            :image-classes ["rounded-md"] :hide-label? true]]
          [:div.flex.flex-col.flex-1.gap-y-1.relative
           [:h1.text-sm.font-bold.text-neutral-600.dark:text-neutral-400
            (str (when edit-modal "LOCAL ") "PLAYLIST")]
           [:div.flex.flex-col.gap-y-6
            [layout/content-header name]
            (when (seq related-streams)
              [:div.flex.gap-x-3.items-center
               [layout/primary-button "Play All"
                #(rf/dispatch [:playlist/play-all related-streams])
                [:i.fa-solid.fa-play]]
               [layout/secondary-button "Shuffle"
                #(rf/dispatch [:playlist/shuffle-all related-streams])
                [:i.fa-solid.fa-shuffle]]
               [:div.hidden.xs:block
                [metadata-popover playlist edit-modal]]])
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
         [items/related-streams related-streams next-page @!layout
          #(rf/dispatch [:playlist/fetch-paginated url next-page])]]))))

(defn playlist-page
  [{{:keys [url]} :query-params}]
  [playlist @(rf/subscribe [:playlist]) url])
