(ns tubo.components.layout
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [tubo.utils :as utils]
   [svgreq.core :as svgreq]))

(defn thumbnail
  [thumbnail-url route name duration & {:keys [classes rounded?] :or {classes "h-44 xs:h-28" rounded? true}}]
  [:div.flex.py-2.box-border {:class classes}
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full.z-10 {:href route :title name}]
    (if thumbnail-url
      [:img.object-cover.min-h-full.max-h-full.min-w-full {:src thumbnail-url :class (when rounded? "rounded")}]
      [:div.bg-gray-300.flex.min-h-full.min-w-full.justify-center.items-center.rounded
       [:i.fa-solid.fa-image.text-3xl.text-white]])
    (when duration
      [:div.rounded.p-2.absolute {:style {:bottom 5 :right 5 :background "rgba(0,0,0,.7)" :zIndex "0"}}
       [:p.text-white {:style {:fontSize "14px"}}
        (if (= duration 0)
          "LIVE"
          (utils/format-duration duration))]])]])

(defn logo
  [& {:keys [height width]}]
  (r/create-element
   (svgreq/embed "resources/public/icons" "tubo" nil)
   (js-obj "height" width "width" height)))

(defn loading-icon
  [service-color & styles]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fas.fa-circle-notch.fa-spin
    {:class (apply str (if (> (count styles) 1) (interpose " " styles) styles))
     :style {:color service-color}}]])

(defn focus-overlay [on-click active? transparent?]
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
       [:div.flex.flex-col.flex-auto.w-full {:class "lg:w-4/5 xl:w-3/5"}
        (map-indexed #(with-meta %2 {:key %1}) children)])]))

(defn content-header
  [heading & children]
  [:div.flex.items-center.justify-between.mt-6
   [:h1.text-3xl.line-clamp-1.mr-6.font-semibold
    {:title heading}
    heading]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn uploader-avatar
  [source name & url]
  (let [image [:img.flex-auto.rounded-full.object-cover.max-w-full.min-h-full {:src source :alt name}]]
    (when source
      [:div.relative.w-12.xs:w-16.h-12.xs:h-16.flex-auto.flex.items-center.shrink-0
       (if url
         [:a.flex-auto.flex.min-h-full.min-w-full.max-h-full.max-w-full {:href url :title name} image]
         image)])))

(defn primary-button
  [label on-click left-icon right-icon]
  [:button.dark:bg-white.bg-stone-800.px-4.rounded-3xl.py-1.outline-none.focus:ring-transparent.whitespace-nowrap
   {:on-click on-click}
   (when left-icon
     [:i.text-neutral-300.dark:text-neutral-800.text-sm
      {:class left-icon}])
   [:span.mx-2.text-neutral-300.dark:text-neutral-900.font-bold.text-sm label]
   (when right-icon
     [:i.text-neutral-300.dark:text-neutral-800.text-sm
      {:class right-icon}])])

(defn secondary-button
  [label on-click left-icon right-icon]
  [:button.dark:bg-transparent.bg-neutral-100.px-4.rounded-3xl.py-1.border.border-neutral-300.dark:border-stone-700.outline-none.focus:ring-transparent.whitespace-nowrap
   {:on-click on-click}
   (when left-icon
     [:i.text-neutral-500.dark:text-white.text-sm
      {:class left-icon}])
   [:span.mx-2.text-neutral-500.dark:text-white.font-bold.text-sm label]
   (when right-icon
     [:i.text-neutral-500.dark:text-white.text-sm
      {:class right-icon}])])

(defn generic-input [label & children]
  [:div.w-full.flex.justify-between.items-center.py-2.gap-x-4
   [:label label]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn text-input
  [label key value on-change placeholder]
  [generic-input label
   [:input.text-black
    {:type          "text"
     :default-value value
     :on-change     on-change
     :placeholder   placeholder}]])

(defn boolean-input
  [label key value on-change]
  [generic-input label
   [:input
    {:type      "checkbox"
     :checked   value
     :value     value
     :on-change on-change}]])

(defn select-input
  [label key value options on-change]
  [generic-input label
   [:select.focus:ring-transparent.bg-transparent.font-bold
    {:value     value
     :on-change on-change}
    (for [[i option] (map-indexed vector options)]
      ^{:key i} [:option.dark:bg-neutral-900.border-none {:value option :key i} option])]])

(defn menu-item
  [{:keys [label icon on-click link] :as item}]
  (let [content [:<>
                 [:span.text-xs.min-w-4.w-4.flex.justify-center.items-center icon]
                 [:span.whitespace-nowrap label]]
        classes ["relative ""flex" "items-center" "gap-x-3" "hover:bg-neutral-200"
                 "dark:hover:bg-stone-800" "py-2" "px-3" "rounded"]]
    (if link
      [:a {:href  (:route link) :target (when (:external? link) "_blank")
           :class (clojure.string/join " " classes)}
       content]
      [:li {:on-click on-click
            :class    (clojure.string/join " " classes)}
       (if (vector? item) item content)])))

(defn menu
  [active? items & {:keys [right top bottom left] :or {right "15px" top "0px"}}]
  (when-not (empty? (remove nil? items))
    [:ul.absolute.bg-neutral-100.dark:bg-neutral-900.border.border-neutral-300.dark:border-stone-700.rounded-t.rounded-b.z-20.p-2.flex.flex-col
     {:class (when-not active? "hidden")
      :style {:right right :left left :top top :bottom bottom}}
     (for [[i item] (map-indexed vector (remove nil? items))]
       ^{:key i} [menu-item item])]))

(defn popover-menu
  [!menu-active? items & {:keys [menu-styles extra-classes]}]
  [:div.flex.items-center
   [focus-overlay #(reset! !menu-active? false) @!menu-active? true]
   [:button.focus:outline-none.relative.pl-4
    {:on-click #(reset! !menu-active? (not @!menu-active?))
     :class extra-classes}
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
      {:class    (if open? "fa-chevron-up" "fa-chevron-down")
       :on-click on-open}]]
    right-button]
   (when open?
     (map-indexed #(with-meta %2 {:key %1}) content))])

(defn show-more-container
  [open? text on-open]
  (let [!text-container (atom nil)
        !resize-observer (atom nil)
        text-clamped? (r/atom nil)]
    (r/create-class
     {:display-name "ShowMoreContainer"
      :component-did-mount
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
        [:div.py-3.flex.flex-wrap.min-w-full
         [:div {:dangerouslySetInnerHTML {:__html text}
                :class                   (when-not open? "line-clamp-2")
                :ref                     #(reset! !text-container %)}]
         (when (or @text-clamped? open?)
           [:div.flex.justify-center.min-w-full.py-4
            [secondary-button
             (if (not open?) "Show More" "Show Less")
             on-open]])])})))
