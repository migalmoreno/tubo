(ns tubo.comments.views
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.layout.views :as layout]
   [tubo.utils :as utils]))

(defn comment-top-metadata
  [{:keys [pinned? uploader-name uploader-url uploader-verified?
           stream-position]}]
  [:div.flex.items-center
   (when pinned?
     [:i.fa-solid.fa-thumbtack.mr-2.text-xs])
   (when uploader-name
     [:div.flex.items-stretch
      [:a
       {:href  (rfe/href :channel-page nil {:url uploader-url})
        :title uploader-name}
       [:h1.text-neutral-800.dark:text-gray-300.font-bold.line-clamp-1
        uploader-name]]
      (when stream-position
        [:div.text-neutral-600.dark:text-neutral-300
         [:span.mx-2.text-xs.whitespace-nowrap
          (utils/format-duration stream-position)]])])
   (when uploader-verified?
     [:i.fa-solid.fa-circle-check.ml-2])])

(defn comment-bottom-metadata
  [{:keys [upload-date like-count hearted-by-uploader? author-avatar
           author-name]}]
  [:div.flex.items-center.my-2
   [:div.mr-4
    [:span {:title upload-date} (utils/format-date-ago upload-date)]]
   (when (and like-count (> like-count 0))
     [:div.flex.items-center.my-2
      [:i.fa-solid.fa-thumbs-up.text-xs]
      [:span.mx-1 {:title like-count} (utils/format-quantity like-count)]])
   (when hearted-by-uploader?
     [:div.relative.w-4.h-4.mx-2
      [:i.fa-solid.fa-heart.absolute.-bottom-1.-right-1.text-xs.text-red-500]
      [:img.rounded-full.object-covermax-w-full.min-h-full
       {:src   author-avatar
        :title (str author-name " hearted this comment")}]])])

(defn comment-item
  [{:keys [id text replies reply-count show-replies] :as comment}]
  [:div.flex.gap-x-4.my-4
   [layout/uploader-avatar comment]
   [:div
    [comment-top-metadata comment]
    [:div.my-2
     [:p
      {:dangerouslySetInnerHTML {:__html text}
       :class                   "[overflow-wrap:anywhere]"}]]
    [comment-bottom-metadata comment]
    [:div.flex.items-center.cursor-pointer
     {:on-click #(rf/dispatch [:comments/toggle-replies id])}
     (when replies
       (if show-replies
         [:<>
          [:p.font-bold "Hide replies"]
          [:i.fa-solid.fa-turn-up.mx-2.text-xs]]
         [:<>
          [:p.font-bold
           (str reply-count (if (= reply-count 1) " reply" " replies"))]
          [:i.fa-solid.fa-turn-down.mx-2.text-xs]]))]]])

(defn comments
  []
  (let [!observer (atom nil)]
    (fn [{:keys [comments next-page]}
         {:keys [uploader-name uploader-avatar url]}]
      (let [service-color       @(rf/subscribe [:service-color])
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
         (if (empty? comments)
           [:div.flex.items-center.flex-auto.flex-col.justify-center.gap-y-4.h-44
            [:i.fa-solid.fa-ghost.text-3xl]
            [:p.text-lg "No available comments"]]
           [:div
            (for [[i {:keys [replies show-replies] :as comment}]
                  (map-indexed vector comments)]
              ^{:key i}
              [:div.flex.flex-col
               {:ref (when (and (seq next-page)
                                (= (+ i 1) (count comments)))
                       last-item-ref)}
               [:div.flex
                [comment-item
                 (assoc comment
                        :author-name   uploader-name
                        :author-avatar uploader-avatar)]]
               (when (and replies show-replies)
                 [:div {:style {:marginLeft "32px"}}
                  (for [[i reply] (map-indexed vector (:items replies))]
                    ^{:key i}
                    [comment-item
                     (assoc reply
                            :author-name   uploader-name
                            :author-avatar uploader-avatar)])])])
            (when (and pagination-loading? (seq next-page))
              [layout/loading-icon service-color :text-md])])]))))
