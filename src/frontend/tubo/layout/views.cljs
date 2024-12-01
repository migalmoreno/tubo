(ns tubo.layout.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reagent.core :as r]
   [svgreq.core :as svgreq]
   [tubo.utils :as utils]))

(defn thumbnail
  [thumbnail-url route name duration & {:keys [classes rounded?]}]
  [:div.flex.box-border {:class classes}
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full.z-10 {:href route :title name}]
    (if thumbnail-url
      [:img.object-cover.min-h-full.max-h-full.min-w-full
       {:src thumbnail-url :class (when rounded? :rounded)}]
      [:div.bg-neutral-300.flex.min-h-full.min-w-full.justify-center.items-center.rounded
       [:i.fa-solid.fa-image.text-3xl.text-white]])
    (when duration
      [:div.rounded.p-1.xs:p-2.absolute.bottom-1.right-1.z-0
       {:class "bg-[rgba(0,0,0,.7)]"}
       [:p.text-white.text-xs.xs:text-base
        (if (= duration 0)
          "LIVE"
          (utils/format-duration duration))]])]])

(defn logo
  [& {:keys [height width]}]
  (r/create-element
   (svgreq/embed "./resources/public/icons" "tubo" nil)
   (js-obj "height" width "width" height)))

(defn loading-icon
  [service-color & classes]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fas.fa-circle-notch.fa-spin
    {:class classes
     :style {:color service-color}}]])

(defn focus-overlay
  [on-click active? transparent?]
  [:div.w-full.fixed.min-h-screen.right-0.top-0.transition-all.delay-75.ease-in-out.z-20
   {:class    (when-not transparent? "bg-black")
    :style    {:visibility (when-not active? "hidden")
               :opacity    (if active? "0.5" "0")}
    :on-click on-click}])

(defn content-container
  [& children]
  (let [page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    [:div.flex.flex-col.flex-auto.items-center.px-5.py-4
     (if page-loading?
       [loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full {:class ["lg:w-4/5" "xl:w-3/5"]}
        (map-indexed #(with-meta %2 {:key %1}) children)])]))

(defn content-header
  [heading & children]
  [:div.flex.items-center.justify-between.mt-6
   [:h1.text-3xl.line-clamp-1.mr-6.font-semibold {:title heading} heading]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn uploader-avatar
  [{:keys [uploader-avatar uploader-name uploader-url]}
   & {:keys [classes] :or {classes ["w-12" "xs:w-16" "h-12" "xs:h-16"]}}]
  (when uploader-avatar
    [:div.relative.flex-auto.flex.items-center.shrink-0.grow-0 {:class classes}
     (conj
      (when uploader-url
        [:a.flex-auto.flex.min-h-full.min-w-full.max-h-full.max-w-full
         {:href  (rfe/href :channel-page nil {:url uploader-url})
          :title uploader-name
          :key   uploader-url}])
      [:img.flex-auto.rounded-full.object-cover.max-w-full.min-h-full
       {:src uploader-avatar :alt uploader-name :key uploader-name}])]))

(defn button
  [label on-click left-icon right-icon &
   {:keys [button-classes label-classes icon-classes]}]
  [:button.px-4.rounded-3xl.py-1.outline-none.focus:ring-transparent.whitespace-nowrap
   {:on-click on-click :class button-classes}
   (when left-icon
     (conj left-icon {:class (or icon-classes label-classes)}))
   [:span.mx-2.font-bold.text-sm {:class label-classes} label]
   (when right-icon
     (conj right-icon {:class (or icon-classes label-classes)}))])

(defn primary-button
  [label on-click left-icon right-icon]
  [button label on-click left-icon right-icon
   :button-classes ["bg-neutral-800" "dark:bg-neutral-200"]
   :label-classes ["text-neutral-300" "dark:text-neutral-900"]])

(defn secondary-button
  [label on-click left-icon right-icon]
  [button label on-click left-icon right-icon
   :button-classes ["bg-neutral-100" "dark:bg-neutral-800"]
   :label-classes ["text-neutral-500" "dark:text-white"]])

(defn generic-input
  [label & children]
  [:div.w-full.flex.justify-between.items-center.py-2.gap-x-4
   [:label label]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn text-input
  [label _key value on-change placeholder]
  [generic-input label
   [:input.text-black
    {:type          "text"
     :default-value value
     :on-change     on-change
     :placeholder   placeholder}]])

(defn boolean-input
  [label _key value on-change]
  [generic-input label
   [:input
    {:type      "checkbox"
     :checked   value
     :value     value
     :on-change on-change}]])

(defn select-input
  [label _key value options on-change]
  [generic-input label
   [:select.focus:ring-transparent.bg-transparent.font-bold
    {:value     value
     :on-change on-change}
    (for [[i option] (map-indexed vector options)]
      ^{:key i}
      [:option.dark:bg-neutral-900.border-none {:value option :key i}
       option])]])

(defn menu-item
  [{:keys [label icon on-click link] :as item}]
  (let [content [:<>
                 (when icon
                   [:span.text-xs.min-w-4.w-4.flex.justify-center.items-center
                    icon])
                 [:span.whitespace-nowrap label]]
        classes ["relative" "flex" "items-center" "gap-x-3"
                 "hover:bg-neutral-200"
                 "dark:hover:bg-neutral-700" "py-2.5" "px-4"
                 "first:rounded-t" "last:rounded-b"]]
    (if link
      [:a
       {:href   (:route link)
        :target (when (:external? link) "_blank")
        :class  (str/join " " classes)}
       content]
      [:li
       {:on-click on-click
        :class    (str/join " " classes)}
       (if (vector? item) item content)])))

(defn menu
  [active? items & {:keys [right top bottom left] :or {right "15px" top "0px"}}]
  (when-not (empty? (remove nil? items))
    [:ul.absolute.bg-neutral-100.dark:bg-neutral-800.rounded-t.rounded-b.z-20.flex.flex-col.text-neutral-800.dark:text-white.shadow.shadow-neutral-400.dark:shadow-neutral-900
     {:class (when-not active? "hidden")
      :style {:right right :left left :top top :bottom bottom}}
     (for [[i item] (map-indexed vector (remove nil? items))]
       ^{:key i} [menu-item item])]))

(defn popover-menu
  [!menu-active? items &
   {:keys [menu-styles extra-classes]
    :or   {menu-styles {:right "25px"} extra-classes [:p-3]}}]
  [:div.flex.items-center
   [focus-overlay #(reset! !menu-active? false) @!menu-active? true]
   [:button.focus:outline-none.relative
    {:on-click #(reset! !menu-active? (not @!menu-active?))
     :class    extra-classes}
    [:i.fa-solid.fa-ellipsis-vertical]
    [menu @!menu-active? items menu-styles]]])

(defn accordeon
  [{:keys [label on-open open? left-icon right-button]} & content]
  [:div.py-4
   [:div.flex.justify-between
    [:div.flex.items-center.text-sm.sm:text-base
     (when left-icon
       [:i.w-6 {:class left-icon}])
     [:h2.mx-4.text-lg.w-24 label]
     [:i.fa-solid.fa-chevron-up.cursor-pointer.text-sm
      {:class    (if open? :fa-chevron-up :fa-chevron-down)
       :on-click on-open}]]
    right-button]
   (when open?
     (map-indexed #(with-meta %2 {:key %1}) content))])

(defn show-more-container
  [_open? _text _on-open]
  (let [!text-container  (atom nil)
        !resize-observer (atom nil)
        text-clamped?    (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (when @!text-container
          (.observe
           (reset! !resize-observer
             (js/ResizeObserver.
              #(let [target (.-target (first %))]
                 (reset! text-clamped?
                   (> (.-scrollHeight target)
                      (.-clientHeight target))))))
           @!text-container)))
      :component-will-unmount
      #(when (and @!resize-observer @!text-container)
         (.unobserve @!resize-observer @!text-container))
      :reagent-render
      (fn [open? text on-open]
        [:div.py-3.min-w-full
         [:span.text-clip.pr-2
          {:dangerouslySetInnerHTML {:__html text}
           :class                   (when-not open? "line-clamp-2")
           :ref                     #(reset! !text-container %)}]
         (when (or @text-clamped? open?)
           [:button.font-bold {:on-click on-open}
            (str "show " (if open? "less" "more"))])])})))

(defn error
  [{:keys [_failure parse-error status status-text]} cb]
  [:div.flex.flex-auto.h-full.items-center.justify-center.p-8
   [:div.flex.flex-col.gap-y-8.border-border-neutral-300.rounded.dark:border-neutral-700.bg-neutral-300.dark:bg-neutral-800.p-8
    [:div.flex.items-center.gap-2.text-xl
     [:i.fa-solid.fa-circle-exclamation]
     [:h3.font-bold
      (str status " " status-text)]]
    (when parse-error
      [:span (or (:original-text parse-error) (:status-text parse-error))])
    [:div.flex.justify-center.gap-x-6
     [primary-button "Go Back" #(rf/dispatch [:navigation/history-go -1])]
     [secondary-button "Retry" #(rf/dispatch cb)]]]])

(defn tabs
  [tabs]
  (let [!current (r/atom (nth tabs 0))]
    (fn [tabs & {:keys [on-change]}]
      [:div
       (into
        [:ul.w-full.flex.justify-center.items-center]
        (for [[i tab] (map-indexed vector tabs)]
          (let [selected? (= (:id tab) (:id @!current))]
            (when tab
              [:li.flex-auto.flex.justify-center.items-center.font-semibold.border-b-2
               {:class (if selected?
                         "border-neutral-700 dark:border-neutral-100"
                         "!border-transparent")
                :key   i}
               [:button.flex-auto.py-4
                {:on-click (when (not selected?)
                             (fn []
                               (reset! !current tab)
                               (on-change (:id @!current))))}
                (if (:label-fn tab)
                  ((:label-fn tab) (:label tab))
                  (:label tab))]]))))])))
