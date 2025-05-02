(ns tubo.bookmarks.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.modals.views :as ms]))

(defn bookmarks
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn []
      (let [bookmarks @(rf/subscribe [:bookmarks])
            items     (map
                       #(assoc %
                               :stream-count (count (:items %))
                               :playlist-id  (or (:playlist-id %) (:id %))
                               :url          (rfe/href :bookmark-page
                                                       nil
                                                       {:id (or (:playlist-id %)
                                                                (:id %))}))
                       bookmarks)]
        [layout/content-container
         [layout/content-header "Bookmarks"
          [:div.flex.flex-auto
           [layout/popover
            [{:label    "Add New"
              :icon     [:i.fa-solid.fa-plus]
              :on-click #(rf/dispatch [:modals/open [modals/add-bookmark]])}
             {:destroy-tooltip-on-click? false
              :custom-content
              [:<>
               [:input.hidden
                {:id        "file-selector"
                 :type      "file"
                 :multiple  true
                 :on-change #(rf/dispatch [:bookmarks/import %])}]
               [:label.whitespace-nowrap.cursor-pointer.w-full.h-full.absolute.right-0.top-0
                {:for "file-selector"}]
               [:span.text-xs.w-10.min-w-4.w-4.flex.items-center
                [:i.fa-solid.fa-file-import]]
               [:span "Import"]]}
             {:label    "Export"
              :icon     [:i.fa-solid.fa-file-export]
              :on-click #(rf/dispatch [:bookmarks/export])}
             {:label    "Clear All"
              :icon     [:i.fa-solid.fa-trash]
              :on-click #(rf/dispatch [:bookmarks/clear])}]]]
          [items/layout-switcher !layout]]
         [items/related-streams items nil !layout]]))))

(def edit-playlist-validation
  [:map
   [:thumbnail
    [:fn
     {:error/fn (constantly "should be a URL")}
     (fn [value] (if (seq value) (.canParse js/URL value) true))]]
   [:name string?]])

(defn playlist-edit-modal
  [id]
  (let [bookmark @(rf/subscribe [:bookmarks/get-by-id id])]
    [ms/modal-content "Edit playlist"
     [layout/form
      {:validation  edit-playlist-validation
       :on-submit   [:bookmark/edit bookmark]
       :submit-text "Update"
       :extra-opts  {:initial-values (select-keys bookmark [:thumbnail :name])}}
      [(fn [{:keys [values errors normalize-name handle-change handle-blur
                    touched set-values]}]
         [layout/form-field {:label "Thumbnail" :key :thumbnail}
          [:div.flex.justify-center.w-full.py-2
           [:div.relative
            (when (and (seq (values :thumbnail))
                       (nil? (first (:thumbnail errors))))
              [:button.absolute.top-2.right-2.z-20.text-red-500
               {:on-click #(set-values {:thumbnail nil})
                :title    "Remove thumbnail"
                :type     "button"}
               [:i.fa-solid.fa-trash]])
            [layout/thumbnail
             {:thumbnail
              (when (and (seq (values :thumbnail))
                         (nil? (first (:thumbnail errors))))
                (values :thumbnail))}
             nil :classes [:h-44 "w-[250px]"] :rounded? true]]]
          [layout/input
           :name (normalize-name :thumbnail)
           :value (values :thumbnail)
           :on-change handle-change
           :on-blur handle-blur
           :placeholder "URL"]
          (when (touched :thumbnail)
            [layout/error-field (first (:thumbnail errors))])])
       {:name        :name
        :label       "Name"
        :type        :text
        :placeholder "name"}]]]))

(defn bookmark
  []
  (let [!layout (r/atom (:items-layout @(rf/subscribe [:settings])))]
    (fn []
      (let [{{:keys [id]} :query-params} @(rf/subscribe
                                           [:navigation/current-match])
            {:keys [items name]}         @(rf/subscribe [:bookmarks/get-by-id
                                                         id])]
        [layout/content-container
         [layout/content-header name
          [:div.flex.flex-auto
           [layout/popover
            (into
             [{:label    "Edit playlist"
               :icon     [:i.fa-solid.fa-pencil]
               :on-click #(rf/dispatch [:modals/open
                                        [playlist-edit-modal id]])}]
             (when (seq items)
               [{:label    "Add streams to queue"
                 :icon     [:i.fa-solid.fa-headphones]
                 :on-click #(rf/dispatch [:queue/add-n items true])}
                {:label    "Add streams to playlist"
                 :icon     [:i.fa-solid.fa-plus]
                 :on-click #(rf/dispatch [:modals/open
                                          [modals/add-to-bookmark items]])}]))]]
          [items/layout-switcher !layout]]
         [items/related-streams
          (map #(assoc % :type "stream" :playlist-id id) items) nil
          !layout]]))))
