(ns tubo.channel.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.bookmarks.modals :as bm]
   [tubo.modals.views :as modals]
   [tubo.utils :as utils]
   [clojure.string :as str]))

(defn metadata-popover
  [{:keys [related-streams]}]
  (when related-streams
    [layout/popover
     [{:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:queue/add-n related-streams true])}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [bm/add-to-bookmark related-streams]])}]
     :extra-classes ["px-5" "xs:p-3"]
     :tooltip-classes ["right-7" "top-0"]]))

(defn metadata
  []
  (let [!observer          (atom nil)
        !show-description? (r/atom false)]
    (fn [{:keys [avatar name subscriber-count description url] :as channel}]
      (let [sub-btn (if @(rf/subscribe [:subscriptions/subscribed url])
                      [layout/secondary-button "Unsubscribe"
                       #(rf/dispatch [:subscriptions/remove url])]
                      [layout/primary-button "Subscribe"
                       #(rf/dispatch [:subscriptions/add channel])])]
        [:div.flex.flex-col.w-full
         [:div.flex.items-center.my-4.gap-x-4
          [layout/uploader-avatar
           {:uploader-avatar avatar
            :uploader-name   name}
           :classes ["w-24" "xs:w-36" "h-24" "xs:h-36"]]
          [:div.flex.flex-col.gap-y-2.flex-auto
           [:div.flex.items-center.justify-between
            [:div
             [:h1.text-2xl.sm:text-4xl.line-clamp-1.font-bold
              {:title name
               :ref   #(rf/dispatch [:navigation/show-title-on-scroll !observer
                                     %
                                     {:rootMargin "-73px" :threshold 0}])}
              name]
             (when subscriber-count
               [:div.flex.items-center.text-neutral-600.dark:text-neutral-400
                {:class ["text-[0.8rem]"]}
                [:span
                 (str (utils/format-quantity subscriber-count)
                      " subscribers")]])]
            [:div.hidden.xs:block
             [metadata-popover channel]]]
           (when-not (empty? description)
             [:div.text-neutral-600.dark:text-neutral-400.text-sm
              {:class "[overflow-wrap:anywhere]"}
              [layout/show-more-container @!show-description? description
               #(rf/dispatch
                 [:modals/open
                  (r/as-element
                   [modals/modal-content
                    name
                    [:div.flex.flex-col.gap-y-2
                     [:h3.font-bold.text-lg "Description"]
                     [:span.text-sm
                      {:dangerouslySetInnerHTML {:__html description}}]]])])
               :classes ["line-clamp-1"]]])
           [:div.hidden.xs:flex sub-btn]]]
         [:p.contents.xs:hidden sub-btn]]))))

(defn channel
  []
  (let [!active-tab-id (r/atom nil)]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [banner next-page related-streams] :as channel}
            @(rf/subscribe [:channel])
            active-tab (first (filter #(= @!active-tab-id
                                          (-> %
                                              :contentFilters
                                              first))
                                      (:tabs channel)))
            next-page-info (if active-tab
                             (:next-page active-tab)
                             next-page)]
        [layout/content-container
         (when banner
           [:div.flex.justify-center.h-24
            [:img.min-w-full.min-h-full.object-cover.rounded-xl
             {:src banner}]])
         [:div.flex.flex-col.gap-y-4
          [metadata channel]
          (when (seq (:tabs channel))
            [:div.flex.justify-between.items-center.border-b.border-neutral-300.dark:border-neutral-700
             [layout/tabs
              (map (fn [{:keys [contentFilters]}]
                     {:id    (first contentFilters)
                      :label (str/capitalize (first contentFilters))})
                   (:tabs channel))
              :selected-id
              (or @!active-tab-id
                  (get-in (first (:tabs channel)) [:contentFilters 0]))
              :on-change
              #(do (reset! !active-tab-id %)
                   (rf/dispatch [:channel/fetch-tab url %]))]])]
         [items/related-streams
          (or (:related-streams active-tab) related-streams) next-page-info
          (:items-layout @(rf/subscribe [:settings]))
          #(rf/dispatch [:channel/fetch-paginated url @!active-tab-id
                         next-page-info])]]))))
