(ns tubo.stream.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.bookmarks.modals :as modals]
   [tubo.comments.views :as comments]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.player.views :as player]
   [tubo.utils :as utils]))

(def player-elements
  ["PlayToggle" "ProgressControl" "VolumePanel" "CurrentTimeDisplay"
   "TimeDivider" "DurationDisplay" "Spacer" "QualitySelector"
   "PlaybackRateMenuButton" "FullscreenToggle"])

(defn get-player-sources
  [available-streams]
  (reverse (map (fn [{:keys [content format resolution averageBitrate]}]
                  {:src   content
                   :type  "video/mp4"
                   :label (str (or resolution "audio-only") " "
                               format
                               (when-not resolution
                                 (str " " averageBitrate "kbit/s")))})
                available-streams)))

(defn player
  [{:keys [thumbnail-url audio-streams video-streams service-id]}]
  (let [page-loading? @(rf/subscribe [:show-page-loading])]
    (when-not page-loading?
      [:div.flex.flex-col.flex-auto.items-center.xl:py-6.!pb-0
       [:div.flex.flex-col.flex-auto.w-full {:class ["xl:w-3/5"]}
        [:div.flex.justify-center.relative
         {:class "h-[300px] md:h-[450px] lg:h-[600px]"}
         [player/main-player
          {:sources       (get-player-sources (into audio-streams video-streams))
           :poster        thumbnail-url
           :controls      true
           :controlBar    {:children player-elements}
           :preload       "metadata"
           :responsive    true
           :fill          true
           :playbackRates [0.5 1 1.5 2]}
          service-id]]]])))

(defn metadata-popover
  [_]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [service-id url] :as stream}]
      (let [bookmarks @(rf/subscribe [:bookmarks])
            liked?    (some #(= (:url %) url) (-> bookmarks first :items))]
        [layout/popover-menu !menu-active?
         [{:label    "Add to queue"
           :icon     [:i.fa-solid.fa-headphones]
           :on-click #(rf/dispatch [:player/switch-to-background stream])}
          {:label    "Play radio"
           :icon     [:i.fa-solid.fa-tower-cell]
           :on-click #(rf/dispatch [:player/start-radio stream])}
          {:label    (if liked? "Remove favorite" "Favorite")
           :icon     (if liked?
                       [:i.fa-solid.fa-heart
                        {:style {:color (utils/get-service-color service-id)}}]
                       [:i.fa-solid.fa-heart])
           :on-click #(rf/dispatch [(if liked? :likes/remove :likes/add) stream true])}
          {:label "Original"
           :link  {:route url :external? true}
           :icon  [:i.fa-solid.fa-external-link-alt]}
          {:label    "Add to playlist"
           :icon     [:i.fa-solid.fa-plus]
           :on-click #(rf/dispatch [:modals/open [modals/add-to-bookmark stream]])}]]))))

(defn metadata-uploader
  [{:keys [uploader-url uploader-name subscriber-count] :as stream}]
  [:div.flex.items-center
   [layout/uploader-avatar stream]
   [:div.mx-3
    [:a.line-clamp-1.font-semibold
     {:href  (rfe/href :channel-page nil {:url uploader-url})
      :title uploader-name}
     uploader-name]
    (when subscriber-count
      [:div.flex.my-2.items-center
       [:i.fa-solid.fa-users.text-xs]
       [:p.mx-2 (utils/format-quantity subscriber-count)]])]])

(defn metadata-stats
  [{:keys [view-count like-count dislike-count upload-date]}]
  [:div.flex.flex-col.items-end.flex-auto.justify-center
   (when view-count
     [:div.sm:text-base.text-sm.mb-1
      [:i.fa-solid.fa-eye]
      [:span.ml-2 (.toLocaleString view-count)]])
   [:div.flex
    (when like-count
      [:div.items-center.sm:text-base.text-sm
       [:i.fa-solid.fa-thumbs-up]
       [:span.ml-2 (.toLocaleString like-count)]])
    (when dislike-count
      [:div.ml-2.items-center.sm:text-base.text-sm
       [:i.fa-solid.fa-thumbs-down]
       [:span.ml-2 dislike-count]])]
   (when upload-date
     [:div.sm:text-base.text-sm.mt-1.whitespace-nowrap
      [:i.fa-solid.fa-calendar]
      [:span.ml-2 (utils/format-date-string upload-date)]])])

(defn metadata
  [{:keys [name] :as stream}]
  [:<>
   [:div.flex.items-center.justify-between.my-3
    [:h1.text-lg.sm:text-2xl.font-bold.line-clamp-1 {:title name} name]
    [metadata-popover stream]]
   [:div.flex.justify-between.py-2.flex-nowrap
    [metadata-uploader stream]
    [metadata-stats stream]]])

(defn description
  [{:keys [description show-description]}]
  (let [show? (:show-description @(rf/subscribe [:settings]))]
    (when (and show? (not (empty? description)))
      [layout/show-more-container show-description description
       #(rf/dispatch [:stream/toggle-layout :show-description])])))

(defn comments
  [{:keys [comments-page show-comments show-comments-loading url] :as stream}]
  (let [show?         (:show-comments @(rf/subscribe [:settings]))
        service-color @(rf/subscribe [:service-color])]
    (when (and comments-page (not (empty? (:comments comments-page))) show?)
      [layout/accordeon
       {:label     "Comments"
        :on-open   #(if show-comments
                      (rf/dispatch [:stream/toggle-layout :show-comments])
                      (if comments-page
                        (rf/dispatch [:stream/toggle-layout :show-comments])
                        (rf/dispatch [:comments/fetch-page url])))
        :open?     show-comments
        :left-icon "fa-solid fa-comments"}
       (if show-comments-loading
         [layout/loading-icon service-color "text-2xl"]
         (when (and show-comments comments-page)
           [comments/comments comments-page stream]))])))

(defn suggested
  [_]
  (let [!menu-active? (r/atom nil)]
    (fn [{:keys [related-streams show-related]}]
      (let [show? (:show-related @(rf/subscribe [:settings]))]
        (when (and show? (not (empty? related-streams)))
          [layout/accordeon
           {:label        "Suggested"
            :on-open      #(rf/dispatch [:stream/toggle-layout :show-related])
            :open?        (not show-related)
            :left-icon    "fa-solid fa-list"
            :right-button [layout/popover-menu !menu-active?
                           [{:label    "Add to queue"
                             :icon     [:i.fa-solid.fa-headphones]
                             :on-click #(rf/dispatch [:queue/add-n related-streams true])}
                            {:label    "Add to playlist"
                             :icon     [:i.fa-solid.fa-plus]
                             :on-click #(rf/dispatch [:modals/open [modals/add-to-bookmark related-streams]])}]]}
           [items/related-streams related-streams nil]])))))

(defn stream
  []
  (let [stream @(rf/subscribe [:stream])]
    [:<>
     [player stream]
     [layout/content-container
      [metadata stream]
      [description stream]
      [comments stream]
      [suggested stream]]]))