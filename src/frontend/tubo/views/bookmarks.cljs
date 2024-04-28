(ns tubo.views.bookmarks
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.modal :as modal]
   [tubo.components.modals.bookmarks :as bookmarks]
   [tubo.events :as events]))

(defn add-bookmark-modal
  []
  (let [!bookmark-name (r/atom "")]
    (fn []
      [modal/modal-content "Create New Playlist?"
       [layout/text-input "Title" :text-input @!bookmark-name
        #(reset! !bookmark-name (.. % -target -value)) "Playlist name"]
       [layout/secondary-button "Cancel"
        #(rf/dispatch [::events/close-modal])]
       [layout/primary-button "Create Playlist"
        #(rf/dispatch [::events/add-bookmark-list {:name @!bookmark-name} true])]])))

(defn bookmarks-page
  []
  (let [!menu-active? (r/atom nil)]
    (fn []
      (let [service-color @(rf/subscribe [:service-color])
            bookmarks     @(rf/subscribe [:bookmarks])
            items         (map #(assoc %
                                       :url (rfe/href :tubo.routes/bookmark nil {:id (:id %)})
                                       :thumbnail-url (-> % :items first :thumbnail-url)
                                       :stream-count (count (:items %))
                                       :bookmark-id (:id %)) bookmarks)]
        [layout/content-container
         [layout/content-header "Bookmarks"
          [layout/popover-menu !menu-active?
           [{:label    "Add New"
             :icon     [:i.fa-solid.fa-plus]
             :on-click #(rf/dispatch [::events/open-modal [add-bookmark-modal]])}
            [:<>
             [:input.hidden
              {:id        "file-selector"
               :type      "file"
               :multiple  "multiple"
               :on-click  #(reset! !menu-active? true)
               :on-change #(rf/dispatch [::events/import-bookmark-lists (.. % -target -files)])}]
             [:label.whitespace-nowrap.cursor-pointer.w-full.h-full.absolute.right-0.top-0
              {:for "file-selector"}]
             [:span.text-xs.w-10.min-w-4.w-4.flex.items-center [:i.fa-solid.fa-file-import]]
             [:span "Import"]]
            {:label    "Export"
             :icon     [:i.fa-solid.fa-file-export]
             :on-click #(rf/dispatch [::events/export-bookmark-lists])}
            {:label    "Clear All"
             :icon     [:i.fa-solid.fa-trash]
             :on-click #(rf/dispatch [::events/clear-bookmark-lists])}]]]
         [items/related-streams items]]))))

(defn bookmark-page
  []
  (let [!menu-active? (r/atom nil)]
    (fn []
      (let [bookmarks                    @(rf/subscribe [:bookmarks])
            service-color                @(rf/subscribe [:service-color])
            {{:keys [id]} :query-params} @(rf/subscribe [:current-match])
            {:keys [items name]}         (first (filter #(= (:id %) id) bookmarks))]
        [layout/content-container
         [layout/content-header name
          (when-not (empty? items)
            [layout/popover-menu !menu-active?
             [{:label    "Add to queue"
               :icon     [:i.fa-solid.fa-headphones]
               :on-click #(rf/dispatch [::events/enqueue-related-streams items])}
              {:label    "Add to playlist"
               :icon     [:i.fa-solid.fa-plus]
               :on-click #(rf/dispatch [::events/add-bookmark-list-modal
                                        [bookmarks/add-to-bookmark-list-modal items]])}]])]
         [items/related-streams (map #(assoc % :type "stream" :bookmark-id id) items)]]))))
