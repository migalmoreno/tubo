(ns tubo.playlist.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn metadata-popover
  [{:keys [related-items]} edit-modal]
  (when related-items
    [layout/popover
     [(when edit-modal
        {:label    "Edit playlist"
         :icon     [:i.fa-solid.fa-pencil]
         :on-click #(rf/dispatch [:modals/open edit-modal])})
      {:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:queue/add-n related-items true])}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [modals/add-to-bookmark related-items]])}]
     :extra-classes ["px-5" "xs:px-3"]]))

(defn playlist
  [{:keys [name uploader-name uploader-url stream-count related-items
           next-page]
    :as   playlist} url edit-modal]
  (let [dark-theme?        @(rf/subscribe [:dark-theme])
        color              (if dark-theme? "10,10,10" "245,245,245")
        top-bg-gradient    (str "linear-gradient(to top, rgba("
                                color
                                ",0.5) 10%, rgba("
                                color
                                ",0.3) 65%, rgba("
                                color
                                ",0.2) 75%, transparent 90%)")
        bottom-bg-gradient (str "linear-gradient(to bottom, rgba("
                                color
                                ",0.7) 10%, rgba("
                                color
                                ",0.9) 50%, rgba("
                                color
                                ",1) 90%)")]
    [:div.w-full.flex.flex-col
     [:div.flex.w-full.items-end.p-8.flex-wrap.xs:flex-nowrap.gap-8.relative
      {:style {"--bg-color"    (:thumbnail-color playlist)
               "--bg-gradient" top-bg-gradient}
       :class ["bg-[color:var(--bg-color)]"
               "before:bg-[image:var(--bg-gradient)]"
               "before:absolute" "before:content-['']" "before:top-0"
               "before:right-0" "before:left-0" "before:bottom-0"]}
      [:div.flex.items-center.justify-center.xs:justify-start.w-full.xs:w-auto.relative
       [layout/thumbnail playlist nil :container-classes ["h-52" "w-52"]
        :image-classes ["rounded-md"] :hide-label? true]]
      [:div.flex.flex-col.flex-1.gap-y-1.relative
       [:h1.text-sm.font-medium.text-neutral-800.dark:text-neutral-400
        (str (when edit-modal "LOCAL ") "PLAYLIST")]
       [:div.flex.flex-col.gap-y-6
        [layout/content-header name]
        (when (seq related-items)
          [:div.flex.gap-x-3.items-center
           [layout/primary-button "Play All"
            #(rf/dispatch [:playlist/play-all related-items])
            [:i.fa-solid.fa-play]]
           [layout/secondary-button "Shuffle"
            #(rf/dispatch [:playlist/shuffle-all related-items])
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
                  (if (= stream-count 1) " stream" " streams"))])]]]]]
     [layout/content-container
      [:div.absolute.left-0.right-0.top-0.-z-10
       {:style {"--bg-color"    (:thumbnail-color playlist)
                "--bg-gradient" bottom-bg-gradient}
        :class ["h-[600px]" "max-h-full" "bg-[color:var(--bg-color)]"
                "before:bg-[image:var(--bg-gradient)]"
                "before:absolute" "before:content-['']" "before:top-0"
                "before:right-0" "before:left-0" "before:bottom-0"]}]
      [items/related-items related-items next-page
       (:items-layout @(rf/subscribe [:settings]))
       #(rf/dispatch [:playlist/fetch-paginated url next-page])]]]))

(defn playlist-page
  [{{:keys [url]} :query-params}]
  [playlist @(rf/subscribe [:playlist]) url])
