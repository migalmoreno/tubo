(ns tubo.components.modals.bookmarks
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.modal :as modal]
   [tubo.components.layout :as layout]))

(defn bookmark-list-item
  [{:keys [items id name] :as bookmark} item]
  [:div.flex.w-full.h-24.rounded.cursor-pointer.hover:bg-gray-100.dark:hover:bg-stone-800.px-2
   {:on-click #(rf/dispatch (if (vector? item)
                              [:tubo.events/add-related-streams-to-bookmark-list bookmark item]
                              [:tubo.events/add-to-bookmark-list bookmark item true]))}
   [:div.w-24
    [layout/thumbnail (-> items first :thumbnail-url) nil name nil
     :classes "h-24"]]
   [:div.flex.flex-col.py-4.px-4
    [:h1.line-clamp-1.font-bold {:title name} name]
    [:span.text-sm (str (count items) " streams")]]])

(defn add-bookmark-modal
  [item]
  (let [!bookmark-name (r/atom "")]
    (fn []
      [modal/modal-content "Create New Playlist?"
       [layout/text-input "Title" :text-input @!bookmark-name
        #(reset! !bookmark-name (.. % -target -value)) "Playlist name"]
       [layout/secondary-button "Back"
        #(rf/dispatch [:tubo.events/back-to-bookmark-list-modal item])]
       [layout/primary-button "Create Playlist"
        #(rf/dispatch [:tubo.events/add-bookmark-list-and-back {:name @!bookmark-name} item])
        "fa-solid fa-plus"]])))

(defn add-to-bookmark-list-modal
  [item]
  (let [bookmarks @(rf/subscribe [:bookmarks])]
    [modal/modal-content "Add to Playlist"
     [:div.flex-auto
      [:div.flex.justify-center.items-center.pb-4
       [layout/primary-button "Create New Playlist"
        #(rf/dispatch [:tubo.events/open-modal [add-bookmark-modal item]])
        "fa-solid fa-plus"]]
      [:div.flex.flex-col.gap-y-2.pr-2
       (for [[i bookmark] (map-indexed vector bookmarks)]
         ^{:key i} [bookmark-list-item bookmark item])]]]))
