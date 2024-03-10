(ns tubo.views.bookmarks
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.modal :as modal]
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
        #(rf/dispatch [::events/add-bookmark-list {:name @!bookmark-name}])]])))

(defn bookmarks-page
  []
  (let [!menu-active? (r/atom nil)]
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
         [{:label    "Create playlist"
           :icon     [:i.fa-solid.fa-plus]
           :on-click #(rf/dispatch [::events/open-modal [add-bookmark-modal]])}]]]
       [items/related-streams items]])))

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
               :on-click #(rf/dispatch [::events/enqueue-related-streams items])}]])]
         [items/related-streams (map #(assoc % :type "stream" :bookmark-id id) items)]]))))
