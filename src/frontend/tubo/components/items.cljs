(ns tubo.components.items
  (:require
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [tubo.components.layout :as layout]
   [tubo.events :as events]
   [tubo.util :as util]))

(defn thumbnail
  [thumbnail-url route url name duration & {:keys [classes] :or {classes "h-44 xs:h-28"}}]
  [:div.flex.py-2.box-border {:class classes}
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
  [{:keys [type url name thumbnail-url description subscriber-count
           stream-count verified? key uploader-name uploader-url
           uploader-avatar upload-date short-description view-count
           duration audio-streams video-streams] :as item}
   item-route service-color bookmarks]
  (let [stream? (or (= type "stream") audio-streams video-streams)]
    [:<>
     (when name
       [:div.flex.items-center.my-2
        [:a {:href item-route :title name}
         [:h1.line-clamp-2.my-1 {:class "[overflow-wrap:anywhere]"} name]]
        (when (and verified? (not uploader-url))
          [:i.fa-solid.fa-circle-check.pl-2])])
     [:div.flex.justify-between
      [:div.flex.items-center.my-2
       (if uploader-url
         [:a {:href (rfe/href :tubo.routes/channel nil {:url uploader-url}) :title uploader-name}
          [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2.break-all
           {:class "[overflow-wrap:anywhere]"}
           uploader-name]]
         [:h1.line-clamp-1.text-neutral-800.dark:text-gray-300.font-bold.pr-2 uploader-name])
       (when (and uploader-url verified?)
         [:i.fa-solid.fa-circle-check])]
      [:div.flex.items-center
       (when stream?
         [:button.ml-2.focus:outline-none
          {:on-click #(rf/dispatch [::events/switch-to-audio-player item service-color])}
          [:i.fa-solid.fa-headphones]])
       (when (some #{(dissoc item :key)} bookmarks)
         [:button.ml-4.focus:outline-none
          {:on-click #(rf/dispatch [::events/remove-from-bookmarks (dissoc item :key)])}
          [:i.fa-solid.fa-trash]])]]
     (when (and subscriber-count (not stream?))
       [:div.flex.items-center
        [:i.fa-solid.fa-users.text-xs]
        [:p.mx-2 (util/format-quantity subscriber-count)]])
     (when stream-count
       [:div.flex.items-center
        [:i.fa-solid.fa-video.text-xs]
        [:p.mx-2 (util/format-quantity stream-count)]])
     [:div.flex.my-1.justify-between
      [:p (util/format-date-ago upload-date)]
      (when view-count
        [:div.flex.items-center.h-full.pl-2
         [:i.fa-solid.fa-eye.text-xs]
         [:p.pl-1.5 (util/format-quantity view-count)]])]]))

(defn stream-item
  [{:keys [url name thumbnail-url duration] :as item} service-color bookmarks]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/stream nil {:url url}) url name duration]
   [item-content item (rfe/href :tubo.routes/stream nil {:url url}) service-color bookmarks]])

(defn channel-item
  [{:keys [url name thumbnail-url] :as item} service-color bookmarks]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/channel nil {:url url}) url name nil]
   [item-content item (rfe/href :tubo.routes/channel nil {:url url}) service-color bookmarks]])

(defn playlist-item
  [{:keys [url name thumbnail-url] :as item} service-color bookmarks]
  [:<>
   [thumbnail thumbnail-url (rfe/href :tubo.routes/playlist nil {:url url}) url name nil]
   [item-content item (rfe/href :tubo.routes/playlist nil {:url url}) service-color bookmarks]])

(defn generic-item
  [item service-color bookmarks]
  [:div.w-full.h-80.xs:h-72 {:key key}
   [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
    (case (:type item)
      "stream" [stream-item item service-color bookmarks]
      "channel" [channel-item item service-color bookmarks]
      "playlist" [playlist-item item service-color bookmarks]
      [stream-item item service-color bookmarks])]])

(defn related-streams
  [related-streams next-page-url]
  (let [service-color @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])
        bookmarks @(rf/subscribe [:bookmarks])]
    [:div.flex.flex-col.items-center.flex-auto.my-2.md:my-8
     (if (empty? related-streams)
       [:div.flex.items-center.flex-auto
        [:p "No available streams"]]
       [:div.grid.w-full.gap-x-10.gap-y-5
        {:class "xs:grid-cols-[repeat(auto-fill,_minmax(165px,_1fr))]"}
        (for [[i item] (map-indexed vector related-streams)]
          ^{:key i} [generic-item item service-color bookmarks])])
     (when-not (empty? next-page-url)
       [layout/loading-icon service-color "text-2xl" (when-not pagination-loading? "invisible")])]))
