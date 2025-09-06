(ns tubo.channel.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]
   [tubo.bookmarks.modals :as modals]
   [tubo.utils :as utils]))

(defn metadata-popover
  [{:keys [related-streams url] :as item}]
  (when related-streams
    [layout/popover
     [(if @(rf/subscribe [:subscriptions/subscribed url])
        {:label    "Unsubscribe"
         :icon     [:i.fa-solid.fa-user-minus]
         :on-click #(rf/dispatch [:subscriptions/remove url])}
        {:label    "Subscribe"
         :icon     [:i.fa-solid.fa-user-plus]
         :on-click #(rf/dispatch [:subscriptions/add item])})
      {:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:queue/add-n related-streams true])}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [modals/add-to-bookmark related-streams]])}]
     :extra-classes ["px-5" "xs:p-3"]
     :tooltip-classes ["right-7" "top-0"]]))

(defn metadata
  []
  (let [!observer (atom nil)]
    (fn [{:keys [avatar name subscriber-count] :as channel}]
      [:div.flex.items-center.justify-between
       [:div.flex.items-center.my-4.gap-x-4
        [layout/uploader-avatar
         {:uploader-avatar avatar
          :uploader-name   name}]
        [:div
         [:h1.text-2xl.line-clamp-1.font-semibold
          {:title name
           :ref   #(rf/dispatch [:navigation/show-title-on-scroll !observer %
                                 {:rootMargin "-73px" :threshold 0}])}
          name]
         (when subscriber-count
           [:div.flex.items-center.text-neutral-600.dark:text-neutral-400.text-sm
            [:span
             (str (utils/format-quantity subscriber-count) " subscribers")]])]]
       [:div.hidden.xs:block
        [metadata-popover channel]]])))

(defn channel
  []
  (let [!show-description? (r/atom false)
        !layout            (r/atom (:items-layout @(rf/subscribe [:settings])))
        !active-tab-id     (r/atom nil)]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [banner description next-page related-streams] :as channel}
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
           :selected-id @!active-tab-id
           :on-change
           #(do (reset! !active-tab-id %)
                (rf/dispatch [:channel/fetch-tab url %]))]
          [items/layout-switcher !layout]]
         [items/related-streams
          (or (:related-streams active-tab) related-streams) next-page-info
          @!layout
          #(rf/dispatch [:channel/fetch-paginated url @!active-tab-id
                         next-page-info])]]))))
