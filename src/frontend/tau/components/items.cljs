(ns tau.components.items
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tau.components.loading :as loading]
   [tau.util :as util]
   ["timeago.js" :as timeago]))

(defn stream-item
  [{:keys [url name thumbnail-url upload-author upload-url
           upload-avatar upload-date short-description
           duration view-count uploaded verified? key]}]
  [:div.w-56.h-66.my-2 {:key key}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:div.flex.py-2.box-border.h-28
     [:div.relative.min-w-full
      [:a.absolute.min-w-full.min-h-full.z-50 {:href (rfe/href :tau.routes/stream nil {:url url}) :title name}]
      [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]
      [:div.rounded.p-2.absolute {:style {:bottom 5 :right 5 :background "rgba(0,0,0,.7)" :zIndex "0"}}
       [:p {:style {:fontSize "14px"}}
        (if (= duration 0)
          "LIVE"
          (util/format-duration duration))]]]]
    [:div.my-2
     [:a {:href (rfe/href :tau.routes/stream nil {:url url}) :title name}
      [:h1.line-clamp-2.my-1 name]]]
    (when-not (empty? upload-author)
      [:div.flex.items-center.my-2
       [:a {:href (rfe/href :tau.routes/channel nil {:url upload-url}) :title upload-author}
        [:h1.line-clamp-1.text-gray-300.font-bold.pr-2 upload-author]]
       (when verified?
         [:i.fa-solid.fa-circle-check])])
    [:div.flex.my-1.justify-between
     [:p (if (-> upload-date js/Date.parse js/isNaN)
           upload-date
           (timeago/format upload-date))]
     (when view-count
       [:div.flex.items-center.h-full.pl-2
        [:i.fa-solid.fa-eye.text-xs]
        [:p.pl-1.5 (util/format-quantity view-count)]])]]])

(defn channel-item
  [{:keys [url name thumbnail-url description subscriber-count
           stream-count verified? key]}]
  [:div.w-56.h-64.my-2 {:key key}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:div.flex.min-w-full.py-3.box-border.h-28
     [:div.relative.min-w-full
      [:a.absolute.min-w-full.min-h-full {:href (rfe/href :tau.routes/channel nil {:url url}) :title name}]
      [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]]]
    [:div.overflow-hidden
     [:div.flex.items-center.py-2.box-border
      [:a {:href (rfe/href :tau.routes/channel nil {:url url}) :title name}
       [:h1.line-clamp-1.text-gray-300.font-bold.pr-2 name]]
      (when verified?
        [:i.fa-solid.fa-circle-check])]
     (when subscriber-count
       [:div.flex.items-center
        [:i.fa-solid.fa-users.text-xs]
        [:p.mx-2 subscriber-count]])
     (when stream-count
       [:div.flex.items-center
        [:i.fa-solid.fa-video.text-xs]
        [:p.mx-2 stream-count]])]]])

(defn playlist-item
  [{:keys [url name thumbnail-url upload-author stream-count key]}]
  [:div.w-56.h-64.my-2 {:key key}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:div.flex.min-w-full.py-3.box-border.h-28
     [:div.relative.min-w-full
      [:a.absolute.min-w-full.min-h-full.z-50 {:href (rfe/href :tau.routes/playlist nil {:url url}) :title name}]
      [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]]]
    [:div.overflow-hidden
     [:div
      [:a {:href (rfe/href :tau.routes/playlist nil {:url url}) :title name}
       [:h1.line-clamp-2 name]]]
     [:div.my-2
      [:h1.text-gray-300.font-bold upload-author]]
     [:div.flex.items-center
      [:i.fa-solid.fa-video.text-xs]
      [:p.mx-2 stream-count]]]]])

(defn related-streams
  [related-streams next-page-url]
  (let [service-color @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])]
    [:div.flex.flex-col.justify-center.items-center.flex-auto
     (if (empty? related-streams)
       [:div
        [:p "No available streams"]]
       [:div.flex.justify-center.flex-wrap
        (for [[i item] (map-indexed vector related-streams)
              :let [keyed-item (assoc item :key i)]]
          (case (:type item)
            "stream" [stream-item keyed-item]
            "channel" [channel-item keyed-item]
            "playlist" [playlist-item keyed-item]))])
     (when-not (empty? next-page-url)
       [loading/items-pagination-loading-icon service-color pagination-loading?])]))
