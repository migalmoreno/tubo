(ns tubo.ui
  (:require
   ["motion/react" :refer [AnimatePresence motion]]
   [clojure.string :as str]
   [fork.re-frame :as fork]
   [malli.core :as m]
   [malli.error :as error]
   [nano-id.core :refer [nano-id]]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [svgreq.core :as svgreq]
   [tubo.utils :as utils]))

(defn thumbnail
  [{:keys [duration thumbnail stream-type stream-count short-form-content name
           info-type playlist-type]} route &
   {:keys [container-classes image-classes hide-label?]}]
  [:div.flex
   {:class container-classes}
   [:div.relative.min-w-full
    [:a.absolute.min-w-full.min-h-full
     {:class ["z-[5]"] :href route :title name}]
    (if thumbnail
      [:img.object-cover.min-h-full.max-h-full.min-w-full
       {:src thumbnail :class image-classes}]
      [:div.bg-neutral-300.flex.min-h-full.min-w-full.justify-center.items-center.rounded
       {:class image-classes}
       [:i.fa-solid.fa-image.text-3xl.text-white]])
    (when-not hide-label?
      [:div.rounded-md.p-1.absolute.bottom-1.right-1.z-0.font-medium
       {:class
        (cond
          (= stream-type "LIVE_STREAM") "bg-red-600/80"
          (or short-form-content
              duration
              (some #{info-type} ["PLAYLIST" "BOOKMARK"])
              (= playlist-type "MIX_STREAM"))
          ["bg-black/70"]
          :else "hidden")}
       [:div.text-neutral-300.text-xs.h-full
        (cond
          (= stream-type "LIVE_STREAM") "LIVE"
          short-form-content "SHORTS"
          duration (utils/format-duration duration)
          (or (some #{info-type} ["PLAYLIST" "BOOKMARK"])
              (= playlist-type "MIX_STREAM"))
          [:div.flex.gap-x-1.items-center.px-1
           [:i.fa-solid
            {:class ["text-[0.6rem]"
                     (if (= playlist-type "MIX_STREAM")
                       "fa-tower-broadcast"
                       "fa-list")]}]
           (if (= playlist-type "MIX_STREAM")
             [:span "Mix"]
             (when stream-count
               [:span.font-bold stream-count]))])]])]])

(defn logo
  [& {:keys [height width]}]
  (r/create-element
   (svgreq/embed "./resources/public/icons" "tubo" nil)
   (js-obj "height" width "width" height)))

(defn bullet
  [& {:keys [extra-classes]}]
  [:span.text-neutral-600.dark:text-neutral-400
   {:dangerouslySetInnerHTML {:__html "&bull;"}
    :class                   (into ["text-[0.75rem]"] extra-classes)}])

(defn loading-icon
  [service-color & classes]
  [:div.w-full.flex.justify-center.items-center.flex-auto
   [:i.fa-solid.fa-circle-notch.fa-spin
    {:class classes
     :style {:color service-color}}]])

(defn background-overlay
  []
  (when-let [{:keys [show?] :as overlay} @(rf/subscribe [:layout/bg-overlay])]
    [:> AnimatePresence
     (when show?
       [:> (.-div motion)
        {:class    (into ["w-full" "fixed" "min-h-screen" "right-0" "top-0"]
                         (conj (:extra-classes overlay)
                               (when-not (:transparent? overlay) "bg-black")))
         :animate  {:opacity 0.5}
         :initial  {:opacity 0}
         :exit     {:opacity 0}
         :on-click (:on-click overlay)}])]))

(defn form-container
  [& children]
  [:div.flex-auto.flex.items-center.flex-col.justify-center.w-full.px-10
   [:div.w-full.flex.justify-center.flex-col.items-center.w-full.md:w-96.gap-y-6
    (map-indexed #(with-meta %2 {:key %1}) children)]])

(defn content-container
  [& children]
  (let [sidebar-minimized @(rf/subscribe [:navigation/sidebar-minimized])]
    [:div
     {:class ["flex" "flex-col" "flex-auto" "items-center" "py-4" "px-4"
              "md:px-0" "w-full" "relative"]}
     [:div.flex.flex-col.flex-auto.w-full
      {:class [(if sidebar-minimized "md:w-4/5" "md:w-11/12")]}
      (map-indexed #(with-meta %2 {:key %1}) children)]]))

(defn content-header
  []
  (let [!observer (atom nil)]
    (fn [heading & children]
      [:div.flex.items-center.justify-between
       [:h1.text-3xl.lg:text-4xl.line-clamp-1.mr-6.font-semibold
        {:title heading
         :ref   #(rf/dispatch [:navigation/show-title-on-scroll !observer %
                               {:rootMargin "-73px" :threshold 0}])}
        heading]
       (map-indexed #(with-meta %2 {:key %1}) children)])))

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
      [:img.flex-auto.rounded-full.object-cover.max-w-full.min-h-full.text-xs.line-clamp-2
       {:class "[overflow-wrap:anywhere]"
        :src   uploader-avatar
        :alt   uploader-name
        :key   uploader-name}])]))

(def common-button-classes
  ["flex" "justify-center" "items-center" "gap-x-2" "p-3" "rounded-full"
   "outline-none" "focus:ring-transparent" "whitespace-nowrap" "cursor-pointer"
   "hover:bg-neutral-500/40" "dark:hover:bg-neutral-800"
   "active:bg-neutral-500/40" "dark:active:bg-neutral-800" "transition-colors"])

(defn button
  [label on-click left-icon right-icon &
   {:keys [button-classes label-classes icon-classes extra-button-args]}]
  [:button
   (merge {:on-click on-click
           :class    (concat
                      common-button-classes
                      button-classes
                      (:class extra-button-args))}
          extra-button-args)
   (when left-icon
     (conj left-icon
           (when (or icon-classes label-classes)
             {:class (or icon-classes label-classes)})))
   (when (seq label) [:span.font-medium.text-sm {:class label-classes} label])
   (when right-icon
     (conj right-icon
           (when (or icon-classes label-classes)
             {:class (or icon-classes label-classes)})))])

(defn primary-button
  [label on-click left-icon right-icon extra-button-args]
  [button label on-click left-icon right-icon
   :extra-button-args extra-button-args
   :button-classes
   ["bg-neutral-800" "dark:bg-neutral-200" "hover:dark:bg-neutral-300"
    "hover:bg-neutral-700" "px-4" "py-2"]
   :label-classes ["text-neutral-300" "dark:text-neutral-900"]])

(defn secondary-button
  [label on-click left-icon right-icon extra-button-args]
  [button label on-click left-icon right-icon
   :extra-button-args extra-button-args
   :button-classes
   ["bg-neutral-200" "dark:bg-neutral-900" "hover:bg-neutral-300"
    "hover:dark:bg-neutral-800" "px-4" "py-2"]
   :label-classes ["text-neutral-600" "dark:text-white"]])

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
  [value options on-change & {:keys [multiple?]}]
  [:select.focus:ring-transparent.border-neutral-300.dark:border-neutral-800.rounded-xl.bg-neutral-200.dark:bg-neutral-900
   {:value     value
    :on-change on-change
    :multiple  multiple?}
   (for [[i {:keys [label value] :as option}] (map-indexed vector options)]
     ^{:key i}
     [:option
      {:value (or value option) :key i}
      (or label option)])])

(defn tooltip-item
  [{:keys [label icon on-click link destroy-tooltip-on-click? custom-content
           subschema hide-bg-overlay-on-click? stop-propagation?]
    :or   {destroy-tooltip-on-click? true
           hide-bg-overlay-on-click? true}} tooltip-id]
  (let [content [:<>
                 (when icon
                   [:span.text-xs.min-w-4.w-4.flex.justify-center.items-center
                    icon])
                 [:span.whitespace-nowrap label]]
        classes (into ["relative" "flex" "items-center" "gap-x-3" "py-2.5"
                       "px-4" "first:rounded-t" "last:rounded-b"
                       (when (or on-click subschema) "cursor-pointer")]
                      (when-not custom-content
                        ["hover:bg-neutral-200"
                         "dark:hover:bg-neutral-800/50"]))]
    (if link
      [:a
       {:href     (:route link)
        :target   (when (:external? link) "_blank")
        :class    (str/join " " classes)
        :on-click (fn [e]
                    (when stop-propagation?
                      (.stopPropagation e))
                    (when destroy-tooltip-on-click?
                      (rf/dispatch [:layout/destroy-tooltip-by-id
                                    tooltip-id])))}
       content]
      [:li
       {:on-click (fn [e]
                    (when stop-propagation?
                      (.stopPropagation e))
                    (if subschema
                      (rf/dispatch [:layout/change-tooltip-items tooltip-id
                                    subschema])
                      (do (when on-click (on-click e))
                          (when destroy-tooltip-on-click?
                            (rf/dispatch [:layout/destroy-tooltip-by-id
                                          tooltip-id]))
                          (when hide-bg-overlay-on-click?
                            (rf/dispatch [:layout/hide-bg-overlay])))))
        :class    (str/join " " classes)}
       (or custom-content content)])))

(defn tooltip
  [tooltip-id & {:keys [extra-classes]}]
  (let [{:keys [items]} @(rf/subscribe [:layout/tooltip-by-id tooltip-id])]
    (when (seq (remove nil? items))
      [:> (.-ul motion)
       {:class      (into ["absolute" "bg-neutral-100" "dark:bg-neutral-900"
                           "rounded-t" "rounded-b" "flex" "flex-col"
                           "text-neutral-800" "dark:text-white" "shadow"
                           "shadow-neutral-400" "dark:shadow-neutral-900"
                           "z-30" "text-sm"]
                          extra-classes)
        :animate    {:scale 1}
        :initial    {:scale 0.9}
        :transition {:duration 0.05}}
       (for [[i item] (map-indexed vector (remove nil? items))]
         ^{:key i} [tooltip-item item tooltip-id])])))

(defn mobile-tooltip
  []
  (let [{:keys [id show?]} @(rf/subscribe [:layout/mobile-tooltip])
        {:keys [items extra-classes] :as tooltip-data}
        @(rf/subscribe [:layout/tooltip-by-id id])]
    [:> AnimatePresence
     (when tooltip-data
       [:div
        {:class (str "tooltip-controller-" id)}
        (when (and (seq (remove nil? items)) show?)
          [:> (.-ul motion)
           {:animate    {:y 0}
            :initial    {:y 400}
            :exit       {:y 400}
            :transition {:ease     "easeInOut"
                         :bounce   0.1
                         :duration 0.3}
            :class      (concat ["bg-neutral-100" "dark:bg-neutral-900" "z-40"
                                 "rounded-t" "rounded-b" "flex" "flex-col"
                                 "text-neutral-800" "dark:text-white"
                                 "shadow" "shadow-neutral-400" "fixed"
                                 "dark:shadow-neutral-900" "bottom-4" "left-2"
                                 "right-2"]
                                extra-classes)}
           (for [[i item] (map-indexed vector (remove nil? items))]
             ^{:key i} [tooltip-item item id])])])]))

(defn mobile-panel
  []
  (let [{:keys [id show?]}                            @(rf/subscribe
                                                        [:layout/mobile-panel])
        {:keys [extra-classes view] :as tooltip-data} @(rf/subscribe
                                                        [:layout/panel-by-id
                                                         id])]
    [:> AnimatePresence
     (when tooltip-data
       [:div
        {:class (str "panel-controller-" id)}
        (when (and view show?)
          [:> (.-div motion)
           {:animate    {:y 0}
            :initial    {:y "100%"}
            :exit       {:y "100%"}
            :transition {:ease     "easeInOut"
                         :bounce   0.1
                         :duration 0.3}
            :class      (concat ["fixed" "right-0" "bottom-0" "z-30" "left-0"
                                 "rounded-t-3xl" "shadow" "shadow-neutral-900"
                                 "dark:shadow-none" "dark:border-t"
                                 "dark:border-neutral-700" "mt-1"]
                                extra-classes)}
           [:div.relative.flex.items-center.justify-center.py-4
            [:div.h-1.w-12.rounded-full.bg-neutral-500.dark:bg-neutral-300.text-neutral-300]
            [:button.absolute.right-2.p-1.cursor-pointer.text-xl
             {:class    ["top-1/2" "-translate-y-1/2"]
              :on-click #(rf/dispatch [:layout/destroy-panels-by-ids [id]])}
             [:i.fa-solid.fa-circle-xmark]]]
           [:div {:class ["h-[calc(100%-36px)]"]}
            view]])])]))

(defn popover
  []
  (let [tooltip-id     (nano-id)
        tooltip-data   (rf/subscribe [:layout/tooltip-by-id tooltip-id])
        common-classes ["px-4" "py-2"]]
    (fn [items &
         {:keys [extra-classes icon tooltip-classes responsive?
                 container-classes extra-button-args
                 destroy-on-click-out? stop-propagation? mobile-only?]
          :or   {icon                  [:i.fa-solid.fa-ellipsis-vertical]
                 responsive?           true
                 destroy-on-click-out? true}}]
      [:div.flex
       {:class (concat [(str "tooltip-controller-" tooltip-id)]
                       container-classes)}
       [:div.relative
        {:class (into ["w-full" "items-center"]
                      (if mobile-only?
                        ["hidden"]
                        (if responsive? ["hidden" "xs:flex"] ["flex"])))}
        [button nil
         (fn [e]
           (when stop-propagation?
             (.stopPropagation e))
           (if @tooltip-data
             (rf/dispatch [:layout/destroy-tooltip-by-id tooltip-id])
             (rf/dispatch [:layout/register-tooltip
                           {:items items
                            :id tooltip-id
                            :destroy-on-click-out?
                            destroy-on-click-out?}])))
         icon nil :button-classes (concat common-classes extra-classes)
         :extra-button-args extra-button-args]
        (when @tooltip-data
          [tooltip tooltip-id :extra-classes tooltip-classes])]
       [button nil
        (fn [e]
          (when stop-propagation?
            (.stopPropagation e))
          (if @tooltip-data
            (rf/dispatch [:layout/destroy-tooltip-by-id tooltip-id])
            (rf/dispatch [:layout/show-mobile-tooltip
                          {:items items
                           :id tooltip-id
                           :destroy-on-click-out?
                           destroy-on-click-out?}])))
        icon nil :button-classes
        (concat common-classes
                extra-classes
                (when-not mobile-only?
                  (if responsive? ["xs:hidden"] ["hidden"])))
        :extra-button-args extra-button-args]])))

(defn panel-popover
  []
  (let [panel-id       (nano-id)
        tooltip-data   (rf/subscribe [:layout/panel-by-id panel-id])
        common-classes ["px-4" "py-2"]]
    (fn [view &
         {:keys [extra-classes icon responsive? container-classes
                 extra-panel-classes
                 destroy-on-click-out? stop-propagation? mobile-only?]
          :or   {icon                  [:i.fa-solid.fa-ellipsis-vertical]
                 responsive?           true
                 destroy-on-click-out? true}}]
      [:div.flex
       {:class (concat [(str "panel-controller-" panel-id)] container-classes)}
       [:div.relative
        [button nil
         (fn [e]
           (when stop-propagation?
             (.stopPropagation e))
           (if @tooltip-data
             (rf/dispatch [:layout/destroy-panel-by-id panel-id])
             (rf/dispatch [:layout/show-mobile-panel
                           {:id panel-id
                            :view view
                            :extra-classes extra-panel-classes
                            :destroy-on-click-out?
                            destroy-on-click-out?}])))
         icon nil :button-classes
         (concat common-classes
                 extra-classes
                 (when-not mobile-only?
                   (if responsive? ["xs:hidden"] ["hidden"])))]]])))

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
      (fn [open? text on-open &
           {:keys [classes] :or {classes ["line-clamp-2"]}}]
        [:div.min-w-full
         [:span.text-clip.pr-2
          {:dangerouslySetInnerHTML {:__html text}
           :class                   (when-not open? classes)
           :ref                     #(reset! !text-container %)}]
         (when (or @text-clamped? open?)
           [:button.font-bold {:on-click on-open}
            (str "show " (if open? "less" "more"))])])})))

(defn error-field
  [error]
  (when error
    [:div.bg-red-500.p-2.rounded error]))

(defn error-container
  [{:keys [type body status status-text problem-message]} cb]
  [content-container
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
         [:i.fa-solid.fa-rotate-right]])]]]])

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
                [:li.flex-auto.flex.justify-center.items-center.font-medium.border-b-2
                 {:class (if selected?
                           ["border-neutral-700" "dark:border-neutral-100"]
                           ["!border-transparent" "text-neutral-600"
                            "dark:text-neutral-400"])
                  :key   i}
                 [:button.flex.flex-auto.py-4.items-center.gap-3.justify-center.text-sm.cursor-pointer
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
       [:button.absolute.h-full.right-3.text-sm.cursor-pointer
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
     [:form.flex.flex-col.gap-y-4.w-full {:on-submit handle-submit}
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

(defn item-popover
  [{:keys [audio-streams video-streams info-type url playlist-id uploader-url
           uploader-name uploader-avatars uploader-verified]
    :as   item}]
  (let [items
        (cond
          (or (= info-type "STREAM") audio-streams video-streams)
          [{:label    "Add to queue"
            :icon     [:i.fa-solid.fa-headphones]
            :on-click #(rf/dispatch [:queue/add item true])}
           {:label    "Start radio"
            :icon     [:i.fa-solid.fa-tower-cell]
            :on-click #(rf/dispatch [:bg-player/start-radio item])}
           {:label    "Add to playlist"
            :icon     [:i.fa-solid.fa-plus]
            :on-click #(rf/dispatch [:bookmarks/open-add-modal item])}
           (when @(rf/subscribe [:bookmarks/playlisted url playlist-id])
             {:label    "Remove from playlist"
              :icon     [:i.fa-solid.fa-trash]
              :on-click #(rf/dispatch [:bookmark/remove item])})
           (if @(rf/subscribe [:subscriptions/subscribed uploader-url])
             {:label    "Unsubscribe from channel"
              :icon     [:i.fa-solid.fa-user-minus]
              :on-click #(rf/dispatch [:subscriptions/remove uploader-url])}
             {:label    "Subscribe to channel"
              :icon     [:i.fa-solid.fa-user-plus]
              :on-click #(rf/dispatch [:subscriptions/add
                                       {:url      uploader-url
                                        :name     uploader-name
                                        :verified uploader-verified
                                        :avatars  uploader-avatars}])})
           {:label    "Show channel details"
            :icon     [:i.fa-solid.fa-user]
            :on-click #(rf/dispatch [:navigation/navigate
                                     {:name   :channel-page
                                      :params {}
                                      :query  {:url uploader-url}}])}]
          (= info-type "CHANNEL")
          [(if @(rf/subscribe [:subscriptions/subscribed url])
             {:label    "Unsubscribe"
              :icon     [:i.fa-solid.fa-user-minus]
              :on-click #(rf/dispatch [:subscriptions/remove url])}
             {:label    "Subscribe to channel"
              :icon     [:i.fa-solid.fa-user-plus]
              :on-click #(rf/dispatch [:subscriptions/add item])})]
          (= info-type "PLAYLIST")
          [{:label    "Add to queue"
            :icon     [:i.fa-solid.fa-headphones]
            :on-click #(rf/dispatch [:playlist/fetch-related-items url])}]
          :else [(when @(rf/subscribe [:bookmarks/bookmarked playlist-id])
                   {:label    "Remove playlist"
                    :icon     [:i.fa-solid.fa-trash]
                    :on-click #(rf/dispatch [:bookmarks/remove playlist-id
                                             true])})])]
    (when (not-empty (remove nil? items))
      [popover items
       :extra-classes ["max-xs:p-2.5"]
       :tooltip-classes ["right-5" "top-0"]])))

(defn grid-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified
           textual-upload-date subscriber-count view-count stream-count
           verified info-type]
    :as   item}]
  (let [route (case info-type
                "STREAM"   (rfe/href :stream-page nil {:url url})
                "CHANNEL"  (rfe/href :channel-page nil {:url url})
                "PLAYLIST" (rfe/href :playlist-page nil {:url url})
                url)]
    [:div.flex.flex-col.max-w-full.min-h-full.max-h-full
     [thumbnail item route :container-classes
      (if (= info-type "CHANNEL")
        ["h-36" "w-36" "m-auto"]
        ["py-2" "h-44" "xs:h-40"])
      :image-classes
      (if (= info-type "CHANNEL") ["rounded-full"] ["rounded-lg"])]
     [:div
      [:div.flex.justify-between.my-2
       (when name
         [:div.flex.gap-x-2.font-medium.text-sm
          [:a {:href route :title name}
           [:span.line-clamp-2
            {:class "[overflow-wrap:anywhere]"}
            name]]
          (when (and verified (not uploader-url))
            [:i.fa-solid.fa-circle-check.text-sm])])
       [:div.h-fit
        [item-popover item]]]
      (when uploader-url
        [:div.flex.justify-between.text-neutral-600.dark:text-neutral-400.items-center.my-2.text-sm
         [:div.flex.gap-x-2.items-center
          [uploader-avatar item :classes ["w-6" "h-6"]]
          (conj
           (when uploader-url
             [:a
              {:href  (rfe/href :channel-page nil {:url uploader-url})
               :title uploader-name
               :key   url}])
           [:span.line-clamp-1.break-all.text-xs
            {:class "[overflow-wrap:anywhere]" :title uploader-name :key url}
            uploader-name])
          (when (and uploader-url uploader-verified)
            [:i.fa-solid.fa-circle-check.text-xs])]])
      [:div.text-neutral-600.dark:text-neutral-400.text-xs.flex.flex-col.gap-y-2
       (when (or subscriber-count stream-count)
         [:div.flex.gap-x-2
          (when (and (= info-type "CHANNEL") subscriber-count)
            [:span
             (str (utils/format-quantity subscriber-count) " subscribers")])
          (when (and (= info-type "CHANNEL") subscriber-count stream-count)
            [bullet])
          (when (and (= info-type "CHANNEL") stream-count)
            [:span (str (utils/format-quantity stream-count) " streams")])])
       (when (or textual-upload-date view-count)
         [:div.flex.items-center.my-1.gap-x-1
          (when textual-upload-date
            [:span (utils/format-date-ago textual-upload-date)])
          (when (and textual-upload-date view-count)
            [bullet])
          (when view-count
            [:span (str (utils/format-quantity view-count) " views")])])]]]))

(defn list-item-content
  [{:keys [url name uploader-url uploader-name uploader-verified description
           short-description subscriber-count view-count stream-count verified
           textual-upload-date info-type]
    :as   item} &
   {:keys [author-classes container-classes metadata-classes title-classes
           thumbnail-container-classes thumbnail-image-classes]
    :or   {author-classes              ["[overflow-wrap:anywhere]"
                                        "line-clamp-1" "break-all" "text-xs"]
           container-classes           ["xs:mr-2"]
           metadata-classes            ["text-xs" "gap-y-3"]
           thumbnail-container-classes ["py-2" "h-28" "min-w-[150px]"
                                        "max-w-[150px]"
                                        "xs:h-32" "xs:min-w-[175px]"
                                        "xs:max-w-[175px]"
                                        "sm:h-44" "sm:min-w-[300px]"
                                        "sm:max-w-[300px]"
                                        "lg:min-w-[350px]" "lg:max-w-[350px]"
                                        "lg:h-48"]
           thumbnail-image-classes     ["rounded-lg"]
           title-classes               ["[overflow-wrap:anywhere]" "text-sm"
                                        "w-fit"
                                        "mt-2" "line-clamp-2" "sm:text-lg"]}}]
  (let [route (case info-type
                "STREAM"   (rfe/href :stream-page nil {:url url})
                "CHANNEL"  (rfe/href :channel-page nil {:url url})
                "PLAYLIST" (rfe/href :playlist-page nil {:url url})
                url)]
    [:div.flex.gap-x-3.xs:gap-x-5
     [thumbnail item route
      :container-classes thumbnail-container-classes
      :image-classes
      (if (= info-type "CHANNEL")
        ["rounded-full" "!min-w-16" "m-auto"]
        thumbnail-image-classes)]
     [:div.flex.flex-col.flex-auto {:class container-classes}
      [:div.flex.items-center.justify-between.gap-x-2
       (when name
         [:a {:href route :title name}
          [:div {:class title-classes}
           [:span name]
           (when (and verified (not uploader-url))
             [:i.fa-solid.fa-circle-check.pl-3.text-sm.w-fit])]])
       [item-popover item]]
      [:div.flex.flex-col.justify-center.text-neutral-600.dark:text-neutral-400
       {:class metadata-classes}
       (when (or view-count textual-upload-date)
         [:div.flex.items-center.gap-x-1
          (when view-count
            [:<>
             [:div.flex.items-center.h-full.whitespace-nowrap
              [:p {:class metadata-classes}
               (str (utils/format-quantity view-count) " views")]]
             (when textual-upload-date
               [bullet])])
          (when textual-upload-date
            [:span.line-clamp-1 {:class metadata-classes}
             (utils/format-date-ago textual-upload-date)])])
       (when (or uploader-url uploader-name)
         [:div.flex.gap-2.items-center
          [uploader-avatar item :classes ["w-6" "h-6"]]
          (conj
           (when uploader-url
             [:a
              {:href  (rfe/href :channel-page nil {:url uploader-url})
               :title uploader-name
               :key   url}])
           [:h1
            {:class author-classes :title uploader-name :key url}
            uploader-name])
          (when (and uploader-url uploader-verified)
            [:i.fa-solid.fa-circle-check.text-xs])])
       (when (and (= info-type "CHANNEL") (or subscriber-count stream-count))
         [:div.flex.flex-col.xs:flex-row.gap-x-1
          (when (and (= info-type "CHANNEL") subscriber-count)
            [:<>
             [:div.flex.items-center.h-full
              [:p
               (str (utils/format-quantity subscriber-count) " subscribers")]]
             (when stream-count
               [bullet])])
          (when stream-count
            [:span
             (str (utils/format-quantity stream-count) " streams")])])
       (when (or (seq description) (seq short-description))
         [:span.text-xs.line-clamp-1.sm:line-clamp-2.leading-5.max-xs:hidden
          {:class "[overflow-wrap:anywhere]"}
          (or description short-description)])]]]))

(defn items-container
  []
  (let [!observer (atom nil)]
    (fn [related-items next-page layout pagination-fn]
      [:<>
       (for [[i item]
             (map-indexed vector related-items)]
         ^{:key i}
         [:div
          {:ref
           (when (and (seq next-page)
                      (= (+ i 1)
                         (count related-items)))
             #(rf/dispatch
               [:layout/add-intersection-observer
                !observer
                %
                (fn [entries]
                  (when (.-isIntersecting (first
                                           entries))
                    (pagination-fn)))]))}
          (if (and layout (= layout "grid"))
            [grid-item-content item]
            [list-item-content item])])])))

(defn related-items
  [related-items next-page layout pagination-fn]
  (let [service-color       @(rf/subscribe [:service-color])
        pagination-loading? @(rf/subscribe [:show-pagination-loading])]
    [:div.flex.flex-col.flex-auto.my-2.md:my-8
     (if (seq related-items)
       (conj (if (and layout (= layout "grid"))
               [:div.grid.w-full.gap-x-4.gap-y-4
                {:class "xs:grid-cols-[repeat(auto-fill,_minmax(215px,_1fr))]"}]
               [:div.flex.flex-col.gap-x-10])
             [items-container related-items next-page layout pagination-fn])
       [:div.flex.items-center.flex-auto.flex-col.justify-center
        [:span "No available items"]])
     (when (and pagination-loading? (seq next-page))
       [loading-icon service-color :text-md])]))

(def tooltip-class-prefix "tooltip-controller-")
(def panel-class-prefix "panel-controller-")

(defn find-tooltip-controller-class-in-node
  [node class-prefix]
  (when (string? (.-className node))
    (some->> (.-className node)
             (re-find (re-pattern (str class-prefix "([\\w\\-]+)")))
             (first))))

(defn find-tooltip-controller-class
  [node class-prefix]
  (or (find-tooltip-controller-class-in-node node class-prefix)
      (some-> (.-parentNode node)
              (find-tooltip-controller-class class-prefix))))

(defn find-clicked-controller-id
  [node class-prefix]
  (some->
    (find-tooltip-controller-class node class-prefix)
    (str/split class-prefix)
    (second)))
