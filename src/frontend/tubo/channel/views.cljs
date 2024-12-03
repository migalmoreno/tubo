(ns tubo.channel.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn metadata-popover
  [_]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [related-streams]}]
      (when related-streams
        [layout/popover-menu !menu-active?
         [{:label    "Add streams to queue"
           :icon     [:i.fa-solid.fa-headphones]
           :on-click #(rf/dispatch [:queue/add-n related-streams true])}
          {:label    "Add streams to playlist"
           :icon     [:i.fa-solid.fa-plus]
           :on-click #(rf/dispatch [:modals/open
                                    [modals/add-to-bookmark
                                     related-streams]])}]]))))

(defn metadata
  [{:keys [avatars name subscriber-count] :as channel}]
  [:div.flex.items-center.justify-between
   [:div.flex.items-center.my-4.gap-x-4
    [layout/uploader-avatar
     {:uploader-avatars avatars
      :uploader-name    name}]
    [:div
     [:h1.text-2xl.line-clamp-1.font-semibold {:title name} name]
     (when subscriber-count
       [:div.flex.items-center.text-neutral-600.dark:text-neutral-400.text-sm
        [:span
         (str (utils/format-quantity subscriber-count) " subscribers")]])]]
   [:div.hidden.lg:block
    [metadata-popover channel]]])

(defn channel
  [_]
  (let [!show-description? (r/atom false)
        !layout            (r/atom (:items-layout @(rf/subscribe [:settings])))
        !active-tab        (r/atom nil)]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [banners description next-page related-streams] :as channel}
            @(rf/subscribe [:channel])
            next-page-url (:url next-page)
            scrolled-to-bottom? @(rf/subscribe [:scrolled-to-bottom])
            page-loading? @(rf/subscribe [:show-page-loading])]
        (when (and next-page-url scrolled-to-bottom?)
          (rf/dispatch [:channel/fetch-paginated url @!active-tab
                        next-page-url]))
        [:<>
         (when-not page-loading?
           (when banners
             [:div.flex.justify-center.h-24
              [:img.min-w-full.min-h-full.object-cover
               {:src (-> banners
                         last
                         :url)}]]))
         [layout/content-container
          [metadata channel]
          (when-not (empty? description)
            [layout/show-more-container @!show-description? description
             #(reset! !show-description? (not @!show-description?))])
          [:div.flex.justify-between
           [layout/tabs
            (map (fn [tab]
                   {:id    (-> tab
                               :contentFilters
                               first)
                    :label (-> tab
                               :contentFilters
                               first)})
                 (:tabs channel))
            :selected-id @!active-tab
            :on-change
            #(do (reset! !active-tab %)
                 (rf/dispatch [:channel/fetch-tab url %]))]
           [items/layout-switcher !layout]]
          [items/related-streams related-streams next-page-url !layout]]]))))
