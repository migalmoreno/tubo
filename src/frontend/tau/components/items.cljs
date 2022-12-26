(ns tau.components.items
  (:require
   [reitit.frontend.easy :as rfe]))

(defn stream-item
  [id {:keys [url name thumbnail-url upload-author upload-url
              upload-avatar upload-date short-description
              duration view-count uploaded verified?]}]
  [:div.w-56.h-66.my-2 {:key id}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:a.overflow-hidden {:href (rfe/href :tau.routes/stream nil {:url url}) :title name}
     [:div.flex.py-3.box-border.h-28
      [:div.relative.min-w-full
       [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]
       [:div.rounded.p-2.absolute {:style {:bottom 5 :right 5 :background "rgba(0,0,0,.7)"}}
        [:p {:style {:fontSize "14px"}}
         (let [duration (js/Date. (* duration 1000))
               slice (if (> (.getHours duration) 1)
                       #(.slice % 11 19)
                       #(.slice % 14 19))]
           (-> duration (.toISOString) slice))]]]]
     [:div.my-2
      [:h1.line-clamp-2.my-1 name]]
     [:a {:href (rfe/href :tau.routes/channel nil {:url upload-url}) :title upload-author}
      [:div.flex.items-center.my-2
       [:h1.line-clamp-1.text-gray-300.font-bold.pr-2 upload-author]
       (when verified?
         [:i.fa-solid.fa-circle-check])]]
     [:div.flex.my-1.justify-between
      [:p (if (-> upload-date js/Date.parse js/isNaN)
            upload-date
            (-> upload-date
                js/Date.parse
                js/Date.
                .toDateString))]
      [:div.flex.items-center.h-full.pl-2
       [:i.fa-solid.fa-eye.text-xs]
       [:p.pl-1.5 (.toLocaleString view-count)]]]]]])

(defn channel-item
  [id {:keys [url name thumbnail-url description subscriber-count stream-count verified?]}]
  [:div.w-56.h-64.my-2 {:key id}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:a.overflow-hidden {:href (rfe/href :tau.routes/channel nil {:url url}) :title name}
     [:div.flex.min-w-full.py-3.box-border.h-28
      [:div.min-w-full
       [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]]]
     [:div.overflow-hidden
      [:div.flex.items-center.my-2
       [:h1.line-clamp-1.text-gray-300.font-bold.pr-2 name]
       (when verified?
         [:i.fa-solid.fa-circle-check])]
      [:div.flex.items-center
       [:i.fa-solid.fa-users.text-xs]
       [:p.mx-2 subscriber-count]]
      [:div.flex.items-center
       [:i.fa-solid.fa-video.text-xs]
       [:p.mx-2 stream-count]]]]]])

(defn playlist-item
  [id {:keys [url name thumbnail-url upload-author stream-count]}]
  [:div.w-56.h-64.my-2 {:key id}
   [:div.px-5.py-2.m-2.flex.flex-col.max-w-full.min-h-full.max-h-full
    [:a.overflow-hidden {:href (rfe/href :tau.routes/playlist nil {:url url}) :title name}
     [:div.flex.min-w-full.py-3.box-border.h-28
      [:div.min-w-full
       [:img.rounded.object-cover.max-h-full.min-w-full {:src thumbnail-url}]]]
     [:div.overflow-hidden
      [:h1.line-clamp-2 name]
      [:h1.text-gray-300.font-bold upload-author]
      [:p (condp >= stream-count
            0 "No streams"
            1 (str stream-count " stream")
            (str stream-count " streams"))]]]]])
