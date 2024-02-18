(ns tubo.components.layout
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn logo []
  [:img.mb-1
   {:src   "/images/tubo.png"
    :style {:maxHeight "25px" :maxWidth "40px"}
    :title "Tubo"}])

(defn loading-icon
  [service-color & styles]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fas.fa-circle-notch.fa-spin
    {:class (apply str (if (> (count styles) 1) (interpose " " styles) styles))
     :style {:color service-color}}]])

(defn focus-overlay [on-click-cb active?]
  [:div.w-full.fixed.min-h-screen.right-0.top-0.transition-all.delay-75.ease-in-out
   {:class    "bg-black/50"
    :style    {:visibility (when-not active? "hidden")
               :opacity    (if active? "1" "0")}
    :on-click on-click-cb}])

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
   [:h1.text-3xl.font-nunito-semibold.line-clamp-1.mr-6
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
   [:select.focus:ring-transparent.bg-transparent.font-bold.font-nunito
    {:value     value
     :on-change on-change}
    (for [[i option] (map-indexed vector options)]
      ^{:key i} [:option.dark:bg-neutral-900.border-none {:value option :key i} option])]])

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
