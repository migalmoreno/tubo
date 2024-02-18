(ns tubo.views.stream
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.events :as events]
   [tubo.components.items :as items]
   [tubo.components.layout :as layout]
   [tubo.components.comments :as comments]
   [tubo.components.modals.bookmarks :as bookmarks]
   [tubo.components.video-player :as player]
   [tubo.util :as util]))

(defn stream
  [match]
  (let [!stream-menu-active? (r/atom nil)
        !suggested-menu-active? (r/atom nil)]
    (fn []
      (let [{:keys [name url video-streams audio-streams view-count
                    subscriber-count like-count dislike-count
                    description uploader-avatar uploader-name
                    uploader-url upload-date related-streams
                    thumbnail-url show-comments-loading comments-page
                    show-comments show-related show-description service-id]
             :as   stream}    @(rf/subscribe [:stream])
            {show-comments?    :show-comments
             show-related?     :show-related
             show-description? :show-description}
            @(rf/subscribe [:settings])
            available-streams (apply conj audio-streams video-streams)
            page-loading?     @(rf/subscribe [:show-page-loading])
            service-color     @(rf/subscribe [:service-color])
            bookmarks         @(rf/subscribe [:bookmarks])
            liked?            (some #(= (:url %) url) (-> bookmarks first :items))
            sources           (reverse (map (fn [{:keys [content format resolution averageBitrate]}]
                                              {:src   content
                                               :type  "video/mp4"
                                               :label (str (or resolution "audio-only") " "
                                                           format
                                                           (when-not resolution
                                                             (str " " averageBitrate "kbit/s")))})
                                            available-streams))
            player-elements   ["playToggle" "progressControl"
                               "volumePanel" "playbackRateMenuButton"
                               "QualitySelector" "fullscreenToggle"]]
        [layout/content-container
         [:div.flex.justify-center.relative
          {:class "h-[300px] md:h-[450px] lg:h-[600px]"}
          [player/player
           {:sources       sources
            :poster        thumbnail-url
            :controls      true
            :controlBar    {:children player-elements}
            :preload       "metadata"
            :responsive    true
            :fill          true
            :playbackRates [0.5 1 1.5 2]}]]
         [:div
          [:div.flex.flex-col
           [:div.flex.items-center.justify-between.pt-4.my-3
            [:div.flex-auto
             [:h1.text-lg.sm:text-2xl.font-nunito-bold.line-clamp-1 name]]
            [:div.flex.flex-auto.justify-end.items-center
             [layout/more-menu !stream-menu-active?
              [{:label    "Add to queue"
                :icon     [:i.fa-solid.fa-headphones]
                :on-click #(rf/dispatch [::events/switch-to-audio-player stream])}
               {:label    (if liked? "Remove favorite" "Favorite")
                :icon     (if liked?
                            [:i.fa-solid.fa-heart {:style {:color (util/get-service-color service-id)}}]
                            [:i.fa-regular.fa-heart])
                :on-click #(rf/dispatch [(if liked? ::events/remove-from-likes ::events/add-to-likes) stream])}
               {:label "Original"
                :link  {:route url :external? true}
                :icon  [:i.fa-solid.fa-external-link-alt]}
               {:label    "Add to playlist"
                :icon     [:i.fa-solid.fa-plus]
                :on-click #(rf/dispatch [::events/add-bookmark-list-modal
                                         [bookmarks/add-to-bookmark-list-modal stream]])}]]]]
           [:div.flex.justify-between.py-2.flex-nowrap
            [:div.flex.items-center
             [layout/uploader-avatar uploader-avatar uploader-name
              (rfe/href :tubo.routes/channel nil {:url uploader-url})]
             [:div.mx-3
              [:a.line-clamp-1.font-nunito-semibold
               {:href (rfe/href :tubo.routes/channel nil {:url uploader-url})}
               uploader-name]
              (when subscriber-count
                [:div.flex.my-2.items-center
                 [:i.fa-solid.fa-users.text-xs]
                 [:p.mx-2 (util/format-quantity subscriber-count)]])]]
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
                [:span.ml-2 (util/format-date-string upload-date)]])]]
           (when (and show-description? (not (empty? description)))
             [layout/show-more-container show-description description
              #(rf/dispatch [::events/toggle-stream-layout :show-description])])
           (when (and comments-page (not (empty? (:comments comments-page))) show-comments?)
             [layout/accordeon
              {:label     "Comments"
               :on-open   #(if show-comments
                             (rf/dispatch [::events/toggle-stream-layout :show-comments])
                             (if comments-page
                               (rf/dispatch [::events/toggle-stream-layout :show-comments])
                               (rf/dispatch [::events/get-comments url])))
               :open?     show-comments
               :left-icon "fa-solid fa-comments"}
              (if show-comments-loading
                [layout/loading-icon service-color "text-2xl"]
                (when (and show-comments comments-page)
                  [comments/comments comments-page uploader-name uploader-avatar url]))])
           (when (and show-related? (not (empty? related-streams)))
             [layout/accordeon
              {:label        "Suggested"
               :on-open      #(rf/dispatch [::events/toggle-stream-layout :show-related])
               :open?        (not show-related)
               :left-icon    "fa-solid fa-list"
               :right-button [layout/more-menu !suggested-menu-active?
                              [{:label    "Add to queue"
                                :icon     [:i.fa-solid.fa-headphones]
                                :on-click #(rf/dispatch [::events/enqueue-related-streams related-streams])}]]}
              [items/related-streams related-streams nil]])]]]))))
