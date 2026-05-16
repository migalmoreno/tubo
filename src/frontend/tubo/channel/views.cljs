(ns tubo.channel.views
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.bookmarks.modals :as bm]
   [tubo.modals.views :as modals]
   [tubo.ui :as ui]
   [tubo.utils :as utils]))

(defn metadata-popover
  [{:keys [related-items]}]
  (when (seq related-items)
    [ui/popover
     [{:label    "Add to queue"
       :icon     [:i.fa-solid.fa-headphones]
       :on-click #(rf/dispatch [:queue/add-n related-items true])}
      {:label    "Add to playlist"
       :icon     [:i.fa-solid.fa-plus]
       :on-click #(rf/dispatch [:modals/open
                                [bm/add-to-bookmark related-items]])}]]))

(defn metadata
  []
  (let [!observer          (atom nil)
        !show-description? (r/atom false)]
    (fn [{:keys [avatar name subscriber-count description url] :as channel}
         active-tab]
      (let [sub-btn (if @(rf/subscribe [:subscriptions/subscribed url])
                      [ui/secondary-button "Unsubscribe"
                       #(rf/dispatch [:subscriptions/remove url])]
                      [ui/primary-button "Subscribe"
                       #(rf/dispatch [:subscriptions/add channel])])]
        [:div.flex.flex-col.w-full
         [:div.flex.items-center.my-4.gap-x-4
          [ui/uploader-avatar
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
                      " subscribers")]])]]
           (when-not (empty? description)
             [:div.text-neutral-600.dark:text-neutral-400.text-sm
              {:class "[overflow-wrap:anywhere]"}
              [ui/show-more-container @!show-description? description
               #(rf/dispatch
                 [:modals/open
                  (r/as-element
                   [modals/modal-content
                    name
                    [:div.flex.flex-col.gap-y-2
                     [:h3.font-bold.text-lg "Description"]
                     [:span.text-sm.whitespace-pre-line
                      {:dangerouslySetInnerHTML {:__html description}}]]])])
               :classes ["line-clamp-1"]]])
           [:div.flex.items-center.gap-x-2
            [:div.hidden.xs:flex sub-btn]
            [:div.hidden.xs:block
             [metadata-popover active-tab]]]]]
         [:p.contents.xs:hidden sub-btn]]))))

(defn channel
  []
  (let [!active-tab-id (r/atom nil)]
    (fn [{{:keys [url]} :query-params}]
      (let [{:keys [banner] :as channel} @(rf/subscribe [:channel])
            active-tab                   (if @!active-tab-id
                                           (first (filter
                                                   #(= @!active-tab-id
                                                       (first
                                                        (:content-filters %)))
                                                   (:tabs channel)))
                                           (first (:tabs channel)))]
        [ui/content-container
         (when banner
           [:div.flex.justify-center.h-24
            [:img.min-w-full.min-h-full.object-cover.rounded-xl
             {:src banner}]])
         [:div.flex.flex-col.gap-y-4
          [metadata channel active-tab]
          (when (seq (:tabs channel))
            [:div.flex.justify-between.items-center.border-b.border-neutral-300.dark:border-neutral-700
             [ui/tabs
              (map (fn [{:keys [content-filters]}]
                     {:id    (first content-filters)
                      :label (str/capitalize (first content-filters))})
                   (:tabs channel))
              :selected-id
              (or @!active-tab-id
                  (get-in (first (:tabs channel)) [:content-filters 0]))
              :on-change
              #(do (reset! !active-tab-id %)
                   (rf/dispatch [:channel/fetch-tab url %]))]])]
         [ui/related-items
          (or (:related-items active-tab) (:related-items channel))
          (:next-page active-tab)
          (:items-layout @(rf/subscribe [:settings]))
          #(rf/dispatch [:channel/fetch-tab-paginated url
                         (or @!active-tab-id
                             (first (:content-filters active-tab)))
                         (:next-page active-tab)])]]))))
