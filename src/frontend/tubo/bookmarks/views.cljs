(ns tubo.bookmarks.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn bookmarks
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn []
      (let [bookmarks @(rf/subscribe [:bookmarks])
            items     (map
                       #(assoc %
                               :stream-count (count (:items %))
                               :bookmark-id  (:id %)
                               :url          (rfe/href :bookmark-page
                                                       nil
                                                       {:id (:id %)})
                               :thumbnails   (-> %
                                                 :items
                                                 first
                                                 :thumbnails))
                       bookmarks)]
        [layout/content-container
         [layout/content-header "Bookmarks"
          [:div.flex.flex-auto
           [layout/popover
            [{:label    "Add New"
              :icon     [:i.fa-solid.fa-plus]
              :on-click #(rf/dispatch [:modals/open [modals/add-bookmark]])}
             [:<>
              [:input.hidden
               {:id        "file-selector"
                :type      "file"
                :multiple  "multiple"
                :on-change #(rf/dispatch [:bookmarks/import
                                          (.. % -target -files)])}]
              [:label.whitespace-nowrap.cursor-pointer.w-full.h-full.absolute.right-0.top-0
               {:for "file-selector"}]
              [:span.text-xs.w-10.min-w-4.w-4.flex.items-center
               [:i.fa-solid.fa-file-import]]
              [:span "Import"]]
             {:label    "Export"
              :icon     [:i.fa-solid.fa-file-export]
              :on-click #(rf/dispatch [:bookmarks/export])}
             {:label    "Clear All"
              :icon     [:i.fa-solid.fa-trash]
              :on-click #(rf/dispatch [:bookmarks/clear])}]]]
          [items/layout-switcher !layout]]
         [items/related-streams items nil !layout]]))))

(defn bookmark
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn []
      (let [bookmarks                    @(rf/subscribe [:bookmarks])
            {{:keys [id]} :query-params} @(rf/subscribe
                                           [:navigation/current-match])
            {:keys [items name]}         (first (filter #(= (:id %) id)
                                                        bookmarks))]
        [layout/content-container
         [layout/content-header name
          (when-not (empty? items)
            [:div.flex.flex-auto
             [layout/popover
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [:queue/add-n items true])}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [:modals/open
                                         [modals/add-to-bookmark items]])}]]])
          [items/layout-switcher !layout]]
         [items/related-streams
          (map #(assoc % :type "stream" :bookmark-id id) items) nil
          !layout]]))))
