(ns tubo.views.bookmarks
  (:require
   [re-frame.core :as rf]
   [tubo.components.items :as items]
   [tubo.components.navigation :as navigation]
   [tubo.events :as events]))

(defn bookmarks-page
  []
  (let [service-color @(rf/subscribe [:service-color])
        bookmarks @(rf/subscribe [:bookmarks])]
    [:div.flex.flex-col.items-center.px-5.py-2.flex-auto
     [:div.flex.flex-col.flex-auto.w-full {:class "ml:w-4/5 xl:w-3/5"}
      [navigation/back-button service-color]
      [:div.flex.justify-between
       [:h1.text-2xl.font-bold.py-6 "Bookmarks"]
       [:button
        {:on-click #(rf/dispatch [::events/enqueue-related-streams bookmarks service-color])}
        [:i.fa-solid.fa-headphones]
        [:span.ml-2.text-neutral-600.dark:text-neutral-300 "Background"]]]
      [items/related-streams bookmarks]]]))
