(ns tubo.layout.views
  (:require
   [clojure.string :as str]
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reagent.core :as r]
   [svgreq.core :as svgreq]
   [tubo.utils :as utils]))

(defn thumbnail
  [thumbnail route name duration & {:keys [classes rounded?]}]
  [:div.flex.box-border {:class classes}
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full.z-10 {:href route :title name}]
    (if thumbnail
      [:img.object-cover.min-h-full.max-h-full.min-w-full
       {:src thumbnail :class (when rounded? :rounded)}]
      [:div.bg-neutral-300.flex.min-h-full.min-w-full.justify-center.items-center.rounded
       [:i.fa-solid.fa-image.text-3xl.text-white]])
    (when duration
      [:div.rounded.p-1.absolute.bottom-1.right-1.z-0
       {:class "bg-[rgba(0,0,0,.7)]"}
       [:p.text-white.text-xs
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
   [:i.fa-solid.fa-circle-notch.fa-spin
    {:class classes
     :style {:color service-color}}]])

(defn background-overlay
  []
  (when-let [{:keys [show?] :as overlay} @(rf/subscribe [:layout/bg-overlay])]
    [:div.w-full.fixed.min-h-screen.right-0.top-0.z-20
     {:class    (conj (:extra-classes overlay)
                      (when-not (:transparent? overlay) "bg-black"))
      :style    {:visibility (when-not show? "hidden")
                 :opacity    (if show? "0.5" "0")}
      :on-click (:on-click overlay)}]))

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
  [{:keys [uploader-avatars uploader-name uploader-url]}
   & {:keys [classes] :or {classes ["w-12" "xs:w-16" "h-12" "xs:h-16"]}}]
  (when (seq uploader-avatars)
    [:div.relative.flex-auto.flex.items-center.shrink-0.grow-0 {:class classes}
     (conj
      (when uploader-url
        [:a.flex-auto.flex.min-h-full.min-w-full.max-h-full.max-w-full
         {:href  (rfe/href :channel-page nil {:url uploader-url})
          :title uploader-name
          :key   uploader-url}])
      [:img.flex-auto.rounded-full.object-cover.max-w-full.min-h-full
       {:src (-> uploader-avatars
                 last
                 :url)
        :alt uploader-name
        :key uploader-name}])]))

(defn button
  [label on-click left-icon right-icon &
   {:keys [button-classes label-classes icon-classes]}]
  [:button.flex.items-center.gap-x-2.px-4.py-2.rounded-full.outline-none.focus:ring-transparent.whitespace-nowrap
   {:on-click on-click :class button-classes}
   (when left-icon
     (conj left-icon {:class (or icon-classes label-classes)}))
   [:span.font-bold.text-sm {:class label-classes} label]
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
   :button-classes ["bg-neutral-200" "dark:bg-neutral-800"]
   :label-classes ["text-neutral-500" "dark:text-white"]])

(defn generic-input
  [label & children]
  [:div.w-full.flex.justify-between.items-center.py-2.gap-x-4
   [:label label]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn form-field
  [{:keys [label orientation]} & children]
  [:div.w-full.flex.py-2.gap-x-4.gap-y-2
   {:class (if (= orientation :horizontal)
             [:flex-col]
             [:justify-between :items-center])}
   [:label label]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn text-input
  [value on-change placeholder]
  [:input.bg-neutral-200.text-neutral-600.dark:text-neutral-300.dark:bg-neutral-950.rounded
   {:type          "text"
    :default-value value
    :on-change     on-change
    :placeholder   placeholder}])

(defn text-field
  [label & args]
  [form-field {:label label :orientation :horizontal} (apply text-input args)])

(defn boolean-input
  [label value on-change]
  [generic-input label
   [:input
    {:type      "checkbox"
     :checked   value
     :value     value
     :on-change on-change}]])

(defn select-input
  [value options on-change]
  [:select.focus:ring-transparent.bg-transparent.font-bold
   {:value     value
    :on-change on-change}
   (for [[i option] (map-indexed vector options)]
     ^{:key i}
     [:option.dark:bg-neutral-900.border-none {:value option :key i}
      option])])

(defn select-field
  [label & args]
  [generic-input label (apply select-input args)])

(defn tooltip-item
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

(defn tooltip
  [items & {:keys [extra-classes]}]
  (when-not (empty? (remove nil? items))
    [:ul.absolute.bg-neutral-100.dark:bg-neutral-800.rounded-t.rounded-b.flex.flex-col.text-neutral-800.dark:text-white.shadow.shadow-neutral-400.dark:shadow-neutral-900.z-30
     {:class (conj extra-classes)}
     (for [[i item] (map-indexed vector (remove nil? items))]
       ^{:key i} [tooltip-item item])]))

(defn mobile-tooltip
  []
  (let [{:keys [id items show?]} @(rf/subscribe [:layout/mobile-tooltip])
        tooltip-data             (rf/subscribe [:layout/tooltip-by-id id])]
    (when @tooltip-data
      [:div.xs:hidden
       {:class    (str "tooltip-controller-" id)
        :on-click #(do (rf/dispatch [:layout/destroy-tooltip-by-id id])
                       (rf/dispatch [:layout/hide-bg-overlay]))}
       (when-not (empty? (remove nil? items))
         [:ul.bg-neutral-100.dark:bg-neutral-800.rounded-t.rounded-b.z-30.flex.flex-col.text-neutral-800.dark:text-white.shadow.shadow-neutral-400.dark:shadow-neutral-900.bottom-4.left-2.right-2.fixed
          {:class (when-not show? "hidden")}
          (for [[i item] (map-indexed vector (remove nil? items))]
            ^{:key i} [tooltip-item item])])])))

(defn popover
  []
  (let [tooltip-id   (nano-id)
        tooltip-data (rf/subscribe [:layout/tooltip-by-id tooltip-id])]
    (fn [items &
         {:keys [extra-classes icon tooltip-classes]
          :or   {extra-classes ["p-3"]
                 icon          [:i.fa-solid.fa-ellipsis-vertical]}}]
      [:div.flex.items-center.tooltip-controller
       {:class (str "tooltip-controller-" tooltip-id)}
       [:button.focus:outline-none.relative.hidden.xs:block
        {:on-click #(if @tooltip-data
                      (rf/dispatch [:layout/destroy-tooltip-by-id tooltip-id])
                      (rf/dispatch [:layout/register-tooltip {:id tooltip-id}]))
         :class    extra-classes}
        icon
        (when @tooltip-data
          [tooltip items :extra-classes tooltip-classes])]
       [:button.focus:outline-none.relative.xs:hidden
        {:on-click #(rf/dispatch [:layout/show-mobile-tooltip
                                  {:items items :id tooltip-id}])
         :class    extra-classes}
        icon]])))

(defn accordeon
  []
  (let [!open? (r/atom false)]
    (fn [{:keys [label on-open open? left-icon]} & content]
      [:div.flex.flex-col.py-4.flex-auto.justify-center
       [:div.flex.justify-center
        [:div.flex.items-center.cursor-pointer.gap-x-2
         {:on-click #(do (when on-open (on-open))
                         (reset! !open? (not @!open?)))}
         left-icon
         [:div.flex.gap-x-4.items-center
          label
          [:i.fa-solid {:class (if @!open? :fa-caret-up :fa-caret-down)}]]]]
       (when @!open?
         [:div.py-4
          (map-indexed #(with-meta %2 {:key %1}) content)])])))

(defn show-more-container
  []
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
        [:div.min-w-full
         [:span.text-clip.pr-2
          {:dangerouslySetInnerHTML {:__html text}
           :class                   (when-not open? "line-clamp-2")
           :ref                     #(reset! !text-container %)}]
         (when (or @text-clamped? open?)
           [:button.font-bold {:on-click on-open}
            (str "show " (if open? "less" "more"))])])})))

(defn error
  [{:keys [type body status status-text problem-message]} cb]
  (let [page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    (if page-loading?
      [loading-icon service-color "text-5xl"]
      [:div.flex.flex-auto.h-full.items-center.justify-center.p-8
       [:div.flex.flex-col.gap-y-8.border-border-neutral-300.rounded.dark:border-neutral-700.w-full
        [:div.flex.items-center.gap-x-4.text-xl
         (cond type
               (case type
                 :success [:i.fa-solid.fa-circle-check]
                 :error   [:i.fa-solid.fa-circle-exclamation]
                 :loading [:div.grow-0 [loading-icon]]
                 [:i.fa-solid.fa-circle-info])
               problem-message [:i.fa-solid.fa-circle-exclamation]
               :else [:i.fa-solid.fa-circle-info])
         [:h3.font-bold
          (cond (or status status-text)
                (str status (when status-text (str " " status-text)))
                problem-message problem-message)]]
        (when-let [message (:message body)]
          [:span.break-words message])
        [:div.bg-neutral-300.dark:bg-neutral-950.py-4.px-4.rounded.relative
         [:div.overflow-x-auto
          [:span.font-mono.text-sm
           (:trace body)]]]
        [:div.flex.justify-center.gap-x-6
         [primary-button "Go back" #(rf/dispatch [:navigation/history-go -1])
          [:i.fa-solid.fa-arrow-left]]
         [secondary-button "Copy strack trace"
          #(rf/dispatch [:copy-to-clipboard (:trace body)])
          [:i.fa-regular.fa-clipboard]]
         [secondary-button "Retry" #(rf/dispatch cb)
          [:i.fa-solid.fa-rotate-right]]]]])))

(defn tabs
  [tabs]
  (let [!current (r/atom (and (seq tabs) (nth tabs 0)))]
    (fn [tabs & {:keys [on-change selected-id]}]
      [:div
       (into
        [:ul.w-full.flex.gap-x-4.justify-center.items-center]
        (when (seq tabs)
          (for [[i tab] (map-indexed vector tabs)]
            (let [selected? (= (:id tab) (or selected-id (:id @!current)))]
              (when tab
                [:li.flex-auto.flex.justify-center.items-center.font-semibold.border-b-2
                 {:class (if selected?
                           "border-neutral-700 dark:border-neutral-100"
                           "!border-transparent")
                  :key   i}
                 [:button.flex.flex-auto.py-4.items-center.gap-3.justify-center
                  {:on-click (when (not selected?)
                               (fn []
                                 (reset! !current tab)
                                 (on-change (:id @!current))))}
                  (:left-icon tab)
                  [:span
                   {:class (when (or (:left-icon tab) (:right-icon tab))
                             ["hidden" "xs:block"])}
                   (if (:label-fn tab)
                     ((:label-fn tab) (:label tab))
                     (:label tab))]
                  (:right-icon tab)]])))))])))
