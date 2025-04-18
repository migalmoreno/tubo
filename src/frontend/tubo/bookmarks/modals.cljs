(ns tubo.bookmarks.modals
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]
   [tubo.modals.views :as modals]))

(defn bookmark-item
  [{:keys [items name] :as bookmark} item]
  [:div.flex.gap-x-4.w-full.h-24.rounded.px-2.cursor-pointer.hover:bg-neutral-100.dark:hover:bg-neutral-800
   {:on-click #(rf/dispatch [(if (vector? item) :bookmark/add-n :bookmark/add)
                             bookmark item true])}
   [layout/thumbnail
    (-> items
        first
        :thumbnails
        last
        :url) nil name nil
    :classes [:h-24 :py-2 "min-w-[125px]" "max-w-[125px]"] :rounded? true]
   [:div.flex.flex-col.py-2
    [:h1.line-clamp-1 {:title name} name]
    [:span.text-xs.text-neutral-600.dark:text-neutral-400
     (str (count items) " streams")]]])

(defn add-bookmark
  [item]
  (let [!bookmark-name (r/atom "")]
    (fn []
      [modals/modal-content "Create New Playlist?"
       [layout/text-field "Title" @!bookmark-name
        #(reset! !bookmark-name (.. % -target -value)) "Playlist name"]
       [layout/secondary-button "Back"
        #(rf/dispatch [:modals/close])]
       [layout/primary-button "Create Playlist"
        #(rf/dispatch [:bookmarks/add {:name @!bookmark-name} item true])
        [:i.fa-solid.fa-plus]]])))

(defn add-to-bookmark
  [item]
  (let [bookmarks @(rf/subscribe [:bookmarks])]
    [modals/modal-content "Add to Playlist"
     [:div.flex-auto
      [:div.flex.justify-center.items-center.pb-4
       [layout/primary-button "Create New Playlist"
        #(rf/dispatch [:modals/open [add-bookmark item]])
        [:i.fa-solid.fa-plus]]]
      [:div.flex.flex-col.gap-y-2.pr-2
       (for [[i bookmark] (map-indexed vector bookmarks)]
         ^{:key i} [bookmark-item bookmark item])]]]))
