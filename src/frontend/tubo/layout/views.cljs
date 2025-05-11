(ns tubo.layout.views
  (:require
   [clojure.string :as str]
   [fork.re-frame :as fork]
   [malli.core :as m]
   [malli.error :as error]
   [nano-id.core :refer [nano-id]]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reagent.core :as r]
   [svgreq.core :as svgreq]
   [tubo.utils :as utils]))

(defn thumbnail
  [{:keys [duration thumbnail stream-type short? name]} route &
   {:keys [classes rounded?]}]
  [:div.flex.box-border {:class classes}
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full {:href route :title name}]
    (if thumbnail
      [:img.object-cover.min-h-full.max-h-full.min-w-full
       {:src thumbnail :class (when rounded? "rounded-md")}]
      [:div.bg-neutral-300.flex.min-h-full.min-w-full.justify-center.items-center.rounded
       [:i.fa-solid.fa-image.text-3xl.text-white]])
    [:div.rounded.p-1.absolute.bottom-1.right-1.z-0
     {:class
      (cond
        (= stream-type "LIVE_STREAM") "bg-red-600/80"
        (or short? duration)          "bg-black/70"
        :else                         "hidden")}
     [:p.text-white.text-xs
      (cond
        (= stream-type "LIVE_STREAM") "LIVE"
        short?                        "SHORTS"
        duration                      (utils/format-duration duration))]]]])

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
  (let [page-loading?     @(rf/subscribe [:show-page-loading])
        service-color     @(rf/subscribe [:service-color])
        sidebar-minimized @(rf/subscribe [:navigation/sidebar-minimized])]
    [:div.flex.flex-col.flex-auto.items-center.px-5.md:p-0.py-4
     (if page-loading?
       [loading-icon service-color "text-5xl"]
       [:div.flex.flex-col.flex-auto.w-full
        {:class [(if sidebar-minimized "md:w-4/5" "md:w-11/12")]}
        (map-indexed #(with-meta %2 {:key %1}) children)])]))

(defn content-header
  [heading & children]
  [:div.flex.items-center.justify-between
   [:h1.text-3xl.line-clamp-1.mr-6.font-extrabold {:title heading} heading]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn uploader-avatar
  [{:keys [uploader-avatar uploader-name uploader-url]}
   & {:keys [classes] :or {classes ["w-12" "xs:w-16" "h-12" "xs:h-16"]}}]
  (when (seq uploader-avatar)
    [:div.relative.flex-auto.flex.items-center.shrink-0.grow-0 {:class classes}
     (conj
      (when uploader-url
        [:a.flex-auto.flex.min-h-full.min-w-full.max-h-full.max-w-full
         {:href  (rfe/href :channel-page nil {:url uploader-url})
          :title uploader-name
          :key   uploader-url}])
      [:img.flex-auto.rounded-full.object-cover.max-w-full.min-h-full
       {:src uploader-avatar
        :alt uploader-name
        :key uploader-name}])]))

(defn button
  [label on-click left-icon right-icon &
   {:keys [button-classes label-classes icon-classes extra-button-args]}]
  [:button.flex.items-center.gap-x-2.px-4.py-2.rounded-full.outline-none.focus:ring-transparent.whitespace-nowrap
   (merge {:on-click on-click :class button-classes} extra-button-args)
   (when left-icon
     (conj left-icon {:class (or icon-classes label-classes)}))
   [:span.font-bold.text-sm {:class label-classes} label]
   (when right-icon
     (conj right-icon {:class (or icon-classes label-classes)}))])

(defn primary-button
  [label on-click left-icon right-icon extra-button-args]
  [button label on-click left-icon right-icon
   :extra-button-args extra-button-args
   :button-classes ["bg-neutral-800" "dark:bg-neutral-200"]
   :label-classes ["text-neutral-300" "dark:text-neutral-900"]])

(defn secondary-button
  [label on-click left-icon right-icon extra-button-args]
  [button label on-click left-icon right-icon
   :extra-button-args extra-button-args
   :button-classes ["bg-neutral-200" "dark:bg-neutral-900"]
   :label-classes ["text-neutral-500" "dark:text-white"]])

(defn form-field
  [{:keys [label orientation] :or {orientation :horizontal}} & children]
  [:div.w-full.flex.py-2.gap-x-4.gap-y-2
   {:class (case orientation
             :horizontal [:flex-col]
             :vertical   [:justify-between :items-center])}
   [:label label]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn input
  [&
   {:keys [type class]
    :or   {type  "text"
           class ["rounded" "bg-neutral-200" "text-neutral-600"
                  "border-neutral-300"
                  "dark:text-neutral-300" "dark:bg-neutral-900"
                  "dark:border-neutral-800"]}
    :as   args}]
  [:input
   (merge
    {:type  type
     :class class}
    args)])

(defn select
  [value options on-change]
  [:select.focus:ring-transparent.border-neutral-300.dark:border-neutral-800.rounded-xl.bg-neutral-200.dark:bg-neutral-900
   {:value     value
    :on-change on-change}
   (for [[i {:keys [label value] :as option}] (map-indexed vector options)]
     ^{:key i}
     [:option
      {:value (or value option) :key i}
      (or label option)])])

(defn tooltip-item
  [{:keys [label icon on-click link destroy-tooltip-on-click? custom-content]
    :or   {destroy-tooltip-on-click? true}} tooltip-id]
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
       {:href     (:route link)
        :target   (when (:external? link) "_blank")
        :class    (str/join " " classes)
        :on-click #(when destroy-tooltip-on-click?
                     (rf/dispatch [:layout/destroy-tooltip-by-id
                                   tooltip-id]))}
       content]
      [:li
       {:on-click #(do (when on-click
                         (on-click %))
                       (when destroy-tooltip-on-click?
                         (rf/dispatch [:layout/destroy-tooltip-by-id
                                       tooltip-id])
                         (rf/dispatch [:layout/hide-bg-overlay])))
        :class    (str/join " " classes)}
       (or custom-content content)])))

(defn tooltip
  [items tooltip-id & {:keys [extra-classes]}]
  (when-not (empty? (remove nil? items))
    [:ul.absolute.bg-neutral-100.dark:bg-neutral-800.rounded-t.rounded-b.flex.flex-col.text-neutral-800.dark:text-white.shadow.shadow-neutral-400.dark:shadow-neutral-900.z-30.cursor-pointer
     {:class (conj extra-classes)}
     (for [[i item] (map-indexed vector (remove nil? items))]
       ^{:key i} [tooltip-item item tooltip-id])]))

(defn mobile-tooltip
  []
  (let [{:keys [id items show?]}
        @(rf/subscribe [:layout/mobile-tooltip])
        tooltip-data (rf/subscribe [:layout/tooltip-by-id id])]
    (when @tooltip-data
      [:div.xs:hidden
       {:class (str "tooltip-controller-" id)}
       (when-not (empty? (remove nil? items))
         [:ul.bg-neutral-100.dark:bg-neutral-800.rounded-t.rounded-b.z-30.flex.flex-col.text-neutral-800.dark:text-white.shadow.shadow-neutral-400.dark:shadow-neutral-900.bottom-4.left-2.right-2.fixed
          {:class (when-not show? "hidden")}
          (for [[i item] (map-indexed vector (remove nil? items))]
            ^{:key i} [tooltip-item item id])])])))

(defn popover
  []
  (let [tooltip-id   (nano-id)
        tooltip-data (rf/subscribe [:layout/tooltip-by-id tooltip-id])]
    (fn [items &
         {:keys [extra-classes icon tooltip-classes responsive?
                 destroy-on-click-out?]
          :or   {extra-classes         ["p-3"]
                 icon                  [:i.fa-solid.fa-ellipsis-vertical]
                 responsive?           true
                 destroy-on-click-out? true}}]
      [:div.flex.items-center.tooltip-controller
       {:class (str "tooltip-controller-" tooltip-id)}
       [:div.relative
        {:class (into ["w-full"]
                      (if responsive? ["hidden" "xs:block"] ["block"]))}
        [:button.focus:outline-none.w-full
         {:class    extra-classes
          :on-click (if @tooltip-data
                      #(rf/dispatch [:layout/destroy-tooltip-by-id tooltip-id])
                      #(rf/dispatch [:layout/register-tooltip
                                     {:id tooltip-id
                                      :destroy-on-click-out?
                                      destroy-on-click-out?}]))}
         icon]
        (when @tooltip-data
          [tooltip items tooltip-id :extra-classes tooltip-classes])]
       [:button.focus:outline-none.relative
        {:on-click (if @tooltip-data
                     #(rf/dispatch [:layout/destroy-tooltip-by-id tooltip-id])
                     #(rf/dispatch [:layout/show-mobile-tooltip
                                    {:items items
                                     :id tooltip-id
                                     :destroy-on-click-out?
                                     destroy-on-click-out?}]))
         :class    (conj extra-classes (if responsive? "xs:hidden" "hidden"))}
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

(defn error-field
  [error]
  (when error
    [:div.bg-red-500.p-2.rounded error]))

(defn error
  [{:keys [type body status status-text problem-message]} cb]
  (let [page-loading? @(rf/subscribe [:show-page-loading])
        service-color @(rf/subscribe [:service-color])]
    [content-container
     (if page-loading?
       [loading-icon service-color "text-5xl"]
       [:div.flex.flex-auto.h-full.items-center.justify-center.py-4
        [:div.flex.flex-col.gap-y-8.border-border-neutral-300.rounded.dark:border-neutral-700.w-full
         [:div.flex.items-center.gap-x-4.text-xl
          {:class (when-not (:message body) :justify-center)}
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
         (when (:trace body)
           [:div.bg-neutral-300.dark:bg-neutral-950.py-4.px-4.rounded.relative
            [:pre.overflow-x-auto.font-mono.text-sm
             (:trace body)]])
         [:div.flex.justify-center.gap-x-6
          [primary-button "Back" #(rf/dispatch [:navigation/history-go -1])
           [:i.fa-solid.fa-arrow-left]]
          (when (:trace body)
            [secondary-button "Copy"
             #(rf/dispatch [:copy-to-clipboard (:trace body)])
             [:i.fa-regular.fa-clipboard]])
          (when cb
            [secondary-button "Retry" #(rf/dispatch cb)
             [:i.fa-solid.fa-rotate-right]])]]])]))

(defn horizontal-tabs
  [tabs]
  (let [!current (r/atom (and (seq tabs) (nth tabs 0)))]
    (fn [tabs & {:keys [on-change selected-id]}]
      [:<>
       [:button.py-2.pr-2
        {:on-click #(on-change nil)
         :class    (if selected-id "block md:hidden" "hidden")}
        [:i.fa-solid.fa-arrow-left]]
       [:div.min-w-fit.w-full.md:w-auto
        {:class (when selected-id "hidden md:block")}
        (into
         [:ul.w-full.flex.flex-col.gap-x-4.justify-center.items-center.gap-y-2]
         (when (seq tabs)
           (for [[i tab] (map-indexed vector tabs)]
             (let [selected? (= (:id tab) (or selected-id (:id @!current)))]
               (when tab
                 [:li.flex-auto.flex.items-center.w-full
                  {:key i}
                  [:button.flex.flex-auto.items-center.gap-6.p-4.flex-shrink-0.flex-auto.rounded-xl.transition-all.ease-in-out.delay-50
                   {:class
                    (if selected?
                      "md:bg-neutral-200 md:dark:bg-neutral-900"
                      "md:bg-transparent hover:md:bg-neutral-200 hover:dark:md:bg-neutral-900")
                    :on-click (fn []
                                (reset! !current tab)
                                (on-change (:id @!current)))}
                   (:left-icon tab)
                   [:span.pr-6
                    (if (:label-fn tab)
                      ((:label-fn tab) (:label tab))
                      (:label tab))]
                   (:right-icon tab)]])))))]])))

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
                 [:button.flex.flex-auto.py-4.items-center.gap-3.justify-center.text-sm.sm:text-base
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

(defn password-input
  []
  (let [!show-password? (r/atom nil)]
    (fn [{:keys [placeholder name] :as field} common-args {:keys [values]}]
      [:div.w-full.relative.flex-auto.flex.flex-col
       [input
        (merge field
               {:type        (if @!show-password? :text :password)
                :placeholder placeholder}
               common-args)]
       [:button.absolute.h-full.right-3.text-sm
        {:on-click #(reset! !show-password? (not @!show-password?))
         :type     :button
         :class    (when-not (seq (values name)) :hidden)}
        [:i.fa-solid {:class (if @!show-password? :fa-eye :fa-eye-slash)}]]])))

(defn form
  [{:keys [validation on-submit submit-text extra-btns extra-opts]} schema]
  [fork/form
   (merge
    {:path             [(keyword (str (nano-id) "-form"))]
     :validation       #(-> (m/explain validation %)
                            (error/humanize))
     :keywordize-keys  true
     :prevent-default? true
     :clean-on-unmount true
     :on-submit        #(rf/dispatch (conj on-submit %))}
    extra-opts)
   (fn [{:keys [values handle-change handle-blur handle-submit errors touched
                submitting? normalize-name]
         :as   fork-args}]
     [:form.flex.flex-col.gap-y-4 {:on-submit handle-submit}
      [:div
       (doall
        (for [[i {:keys [label name] :as field}]
              (map-indexed vector (remove nil? schema))
              :let [common-args {:name      (normalize-name name)
                                 :value     (values name)
                                 :on-change handle-change
                                 :on-blur   handle-blur}]]

          (if (fn? field)
            (field fork-args)
            ^{:key i}
            [form-field {:label label}
             (case (:type field)
               :text     [input
                          (merge {:type :text} field common-args)]
               :password [password-input field common-args fork-args])
             (when (touched name)
               [error-field (first (get errors name))])])))]
      [:div.flex.justify-center.gap-x-2
       extra-btns
       [primary-button submit-text nil nil
        (when submitting?
          [loading-icon nil :text-neutral-300 :dark:text-neutral-900])
        {:disabed (when submitting? "true")}]]])])
