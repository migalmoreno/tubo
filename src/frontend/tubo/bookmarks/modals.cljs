(ns tubo.bookmarks.modals
  (:require
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]
   [tubo.modals.views :as modals]))

(defn bookmark-item
  [{:keys [items name] :as bookmark} item]
  [:div.flex.gap-x-4.w-full.h-24.rounded.px-2.cursor-pointer.hover:bg-neutral-100.dark:hover:bg-neutral-800
   {:on-click #(rf/dispatch [(if (vector? item) :bookmark/add-n :bookmark/add)
                             bookmark item true])}
   [layout/thumbnail bookmark nil :classes
    [:h-24 :py-2 "min-w-[125px]" "max-w-[125px]"] :rounded? true]
   [:div.flex.flex-col.py-2
    [:h1.line-clamp-1 {:title name} name]
    [:span.text-xs.text-neutral-600.dark:text-neutral-400
     (str (count items) " streams")]]])

(defn add-bookmark
  []
  [modals/modal-content "Create New Playlist?"
   [layout/form
    {:validation  [:map [:name string?]]
     :on-submit   [:bookmarks/handle-add-form true]
     :submit-text "Create playlist"
     :extra-btns  [layout/secondary-button "Back"
                   #(rf/dispatch [:modals/close]) nil nil {:type :button}]}
    [{:name        :name
      :label       "Name"
      :type        :text
      :placeholder "Playlist name"}]]])

(defn add-to-bookmark
  [item]
  (let [bookmarks @(rf/subscribe [:bookmarks])]
    [modals/modal-content "Add to Playlist"
     [:div.flex-auto
      [:div.flex.justify-center.items-center.pb-4
       [layout/primary-button "Create New Playlist"
        #(rf/dispatch [:modals/open [add-bookmark]])
        [:i.fa-solid.fa-plus]]]
      [:div.flex.flex-col.gap-y-2.pr-2
       (for [[i bookmark] (map-indexed vector bookmarks)]
         ^{:key i} [bookmark-item bookmark item])]]]))
