(ns tau.components.comments
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.components.loading :as loading]
   [tau.events :as events]
   ["timeago.js" :as timeago]))

(defn comment-item
  [{:keys [id text uploader-name uploader-avatar uploader-url
           upload-date uploader-verified? like-count hearted-by-uploader?
           pinned? replies key]} author-name author-avatar]
  [:div.flex.my-4
   (when uploader-avatar
     [:div.flex.items-center.py-3.box-border.h-12
      [:div.w-12
       [:a {:href (rfe/href :tau.routes/channel nil {:url uploader-url}) :title name}
        [:img.rounded-full.object-cover.min-w-full.min-h-full {:src uploader-avatar}]]]])
   [:div.ml-4
    [:div.flex.items-center
     (when pinned?
       [:i.fa-solid.fa-thumbtack.mr-2.text-xs])
     [:a {:href (rfe/href :tau.routes/channel nil {:url uploader-url}) :title name}
      [:h1.text-gray-300.font-bold uploader-name]]
     (when uploader-verified?
       [:i.fa-solid.fa-circle-check.ml-2])]
    [:div.my-2
     [:p text]]
    [:div..flex.items-center.my-2
     [:div.mr-4
      [:p (if (-> upload-date js/Date.parse js/isNaN)
            upload-date
            (timeago/format upload-date))]]
     (when like-count
       [:div.flex.items-center.my-2
        [:i.fa-solid.fa-thumbs-up.text-xs]
        [:p.mx-1 like-count]])
     (when hearted-by-uploader?
       [:div.relative.w-4.h-4.mx-2
        [:i.fa-solid.fa-heart.absolute.-bottom-1.-right-1.text-xs.text-red-500]
        [:img.rounded-full.object-covermax-w-full.min-h-full
         {:src author-avatar :title (str author-name " hearted this comment")}]])]]])

(defn comments
  [{:keys [comments next-page disabled?]} author-name author-avatar url]
  (let [pagination-loading? @(rf/subscribe [:show-pagination-loading])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col
     [:div
      (for [[i comment] (map-indexed vector comments)]
        [comment-item (assoc comment :key i) author-name author-avatar])]
     (when (:url next-page)
       (if pagination-loading?
         (loading/loading-icon service-color)
         [:div.flex.items-center.justify-center
          {:style {:cursor "pointer"}
           :on-click #(rf/dispatch [::events/comments-pagination url (:url next-page)])}
          [:i.fa-solid.fa-plus]
          [:p.px-2 "Show more comments"]]))]))
