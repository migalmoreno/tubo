(ns tubo.comments.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn comment-top-metadata
  [{:keys [pinned uploader-name uploader-url uploader-verified
           stream-position]}]
  [:div.flex.items-center
   (when pinned
     [:i.fa-solid.fa-thumbtack.mr-2.text-xs])
   (when uploader-name
     [:div.flex.items-stretch
      [:a.text-sm
       {:href  (rfe/href :channel-page nil {:url uploader-url})
        :title uploader-name}
       [:h1.text-neutral-800.dark:text-gray-300.font-bold.line-clamp-1
        uploader-name]]
      (when stream-position
        [:div.text-neutral-600.dark:text-neutral-300
         [:span.mx-2.text-xs.whitespace-nowrap
          (utils/format-duration stream-position)]])])
   (when uploader-verified
     [:i.fa-solid.fa-circle-check.ml-2])])

(defn comment-bottom-metadata
  [{:keys [textual-upload-date like-count hearted-by-uploader author-avatar
           author-name]}]
  [:div.flex.items-center
   [:div.mr-4.text-sm.text-neutral-600.dark:text-neutral-300
    [:span {:title textual-upload-date}
     (utils/format-date-ago textual-upload-date)]]
   (when (and like-count (> like-count 0))
     [:div.flex.items-center
      [:i.fa-solid.fa-thumbs-up.text-xs]
      [:span.mx-1.text-sm.text-neutral-600.dark:text-neutral-300
       {:title like-count} (utils/format-quantity like-count)]])
   (when hearted-by-uploader
     [:div.relative.w-4.h-4.mx-2
      [:i.fa-solid.fa-heart.absolute.-bottom-1.-right-1.text-xs.text-red-500]
      [:img.rounded-full.object-covermax-w-full.min-h-full
       {:src   author-avatar
        :title (str author-name " hearted this comment")}]])])

(defn comment-item
  [{:keys [comment-id comment-text replies-page replies reply-count show-replies
           url reply?]
    :as   comment}]
  [:div.flex.gap-x-4
   [layout/uploader-avatar comment :classes
    (if reply? ["w-8" "h-8"] ["w-12" "h-12"])]
   [:div.flex.flex-col.gap-y-4
    [:div.flex.flex-col.gap-y-2
     [comment-top-metadata comment]
     [:div
      [:p.text-sm
       {:dangerouslySetInnerHTML {:__html (:content comment-text)}
        :class                   "[overflow-wrap:anywhere]"}]]
     [comment-bottom-metadata comment]]
    (when (seq replies-page)
      [:div.flex.items-center.cursor-pointer.gap-x-2
       {:on-click #(rf/dispatch (if (seq replies)
                                  [:comments/show-replies comment-id
                                   (not show-replies)]
                                  [:comments/fetch-replies comment-id url
                                   replies-page]))}
       (if show-replies
         [:<>
          [:span.font-bold.text-sm "Hide replies"]
          [:i.fa-solid.fa-turn-up.mx-2.text-xs]]
         [:<>
          [:span.font-bold.text-sm
           (str reply-count (if (= reply-count 1) " reply" " replies"))]
          [:i.fa-solid.fa-turn-down.mx-2.text-xs]])])]])

(defn comments
  []
  (let [!observer (atom nil)]
    (fn [{:keys [related-items next-page]}
         {:keys [uploader-name uploader-avatar url service-id]}]
      (let [service-color       (utils/get-service-color service-id)
            pagination-loading? @(rf/subscribe [:show-pagination-loading])
            last-item-ref       #(when-not pagination-loading?
                                   (when @!observer (.disconnect @!observer))
                                   (when %
                                     (.observe
                                      (reset! !observer
                                        (js/IntersectionObserver.
                                         (fn [entries]
                                           (when (.-isIntersecting (first
                                                                    entries))
                                             (rf/dispatch
                                              [:comments/fetch-paginated url
                                               next-page])))))
                                      %)))]
        [:div.flex.flex-col
         (if (seq related-items)
           [:div.flex.flex-col.gap-y-6
            (for [[i
                   {:keys [comment-id replies replies-loading show-replies]
                    :as   comment}]
                  (map-indexed vector related-items)]
              ^{:key i}
              [:div.flex.flex-col
               {:ref (when (and (seq next-page)
                                (= (+ i 1) (count related-items)))
                       last-item-ref)}
               [:div.flex
                [comment-item
                 (assoc comment
                        :author-name   uploader-name
                        :author-avatar uploader-avatar
                        :url           url)]]
               (when (and (seq replies) show-replies)
                 [:div.flex.flex-col.gap-y-8.my-4 {:class "ml-[32px]"}
                  (for [[i reply] (map-indexed vector (:items replies))]
                    ^{:key i}
                    [comment-item
                     (assoc reply
                            :reply?        true
                            :author-name   uploader-name
                            :author-avatar uploader-avatar
                            :url           url)])])
               (when (and show-replies
                          (or replies-loading (:next-page replies)))
                 [:div.h-8.flex
                  {:class "ml-[40px]"}
                  (cond
                    replies-loading [layout/loading-icon service-color :text-md]
                    (:next-page replies)
                    [:button
                     {:on-click #(rf/dispatch [:comments/fetch-more-replies
                                               comment-id
                                               url
                                               (:next-page replies)])}
                     [:i.fa-solid.fa-turn-up.mx-2.text-xs.rotate-90]
                     [:span.font-bold.text-sm "Show more replies"]])])])
            (when (and pagination-loading? (seq next-page))
              [layout/loading-icon service-color :text-md])]
           [:div.flex.items-center.flex-auto.flex-col.justify-center.h-44
            [:span "No available comments"]])]))))
