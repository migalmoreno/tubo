(ns tubo.components.items
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.loading :as loading]
   [tubo.util :as util]))

(defn thumbnail
  [thumbnail-url route url name duration]
  [:div.flex.py-2.box-border.h-44.xs:h-28
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full.z-10 {:href route :title name}]
    [:img.rounded.object-cover.min-h-full.max-h-full.min-w-full {:src thumbnail-url}]
    (when duration
      [:div.rounded.p-2.absolute {:style {:bottom 5 :right 5 :background "rgba(0,0,0,.7)" :zIndex "0"}}
       [:p.text-white {:style {:fontSize "14px"}}
        (if (= duration 0)
          "LIVE"
          (util/format-duration duration))]])]])

(defn item-content
  [{:keys [url name thumbnail-url description subscriber-count
           stream-count verified? key uploader-name uploader-url
           uploader-avatar upload-date short-description view-count]} item-route]
  [:<>
   (when name
     [:div.my-2
      [:a {:href item-route :title name}
       [:h1.line-clamp-2.my-1 name]]])
   (when-not (empty? uploader-name)
     [:div.flex.items-center.my-2
      (if uploader-url
        [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
         [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2 uploader-name]]
        [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2 uploader-name])
      (when verified?
        [:i.fa-solid.fa-circle-check])])
   (when subscriber-count
      [:div.flex.items-center
       [:i.fa-solid.fa-users.text-xs]
       [:p.mx-2 subscriber-count]])
   (when stream-count
     [:div.flex.items-center
      [:i.fa-solid.fa-video.text-xs]
      [:p.mx-2 stream-count]])
   [:div.flex.my-1.justify-between
    [:p (util/format-date upload-date)]
    (when view-count
      [:div.flex.items-center.h-full.pl-2
       [:i.fa-solid.fa-eye.text-xs]
       [:p.pl-1.5 (util/format-quantity view-count)]])]])

(defn stream-item
  [{:keys [url name thumbnail-url duration] :as item}]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/stream nil {:url url}) url name duration]
   [item-content item (rfe/href :tubo.routes/stream nil {:url url})]])

(defn channel-item
  [{:keys [url name thumbnail-url] :as item}]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/channel nil {:url url}) url name nil]
   [item-content item (rfe/href :tubo.routes/channel nil {:url url})]])

(defn playlist-item
  [{:keys [url name thumbnail-url] :as item}]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/playlist nil {:url url}) url name nil]
   [item-content item (rfe/href :tubo.routes/playlist nil {:url url})]])

(defn generic-item
  [item]
  [:div.w-full.xs:w-56.h-80.xs:h-72.my-2 {:key key}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    (case (:type item)
      "stream" [stream-item item]
      "channel" [channel-item item]
      "playlist" [playlist-item item])]])

(defn related-streams
  [related-streams next-page-url]
  (let [service-color @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])]
    [:div.flex.flex-col.justify-center.items-center.flex-auto.my-2.md:my-8
     (if (empty? related-streams)
       [:div.flex.items-center
        [:p "No available streams"]]
       [:div.flex.justify-center.flex-wrap
        (for [[i item] (map-indexed vector related-streams)
              :let [keyed-item (assoc item :key i)]]
          [generic-item keyed-item])])
     (when-not (empty? next-page-url)
       [loading/loading-icon service-color "text-2xl" (when-not pagination-loading? "invisible")])]))
