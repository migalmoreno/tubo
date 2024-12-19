(ns tubo.modals.views
  (:require
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]))

(defn modal-content
  [title body & extra-buttons]
  [:div.bg-white.dark:bg-neutral-900.max-h-full.flex.flex-col.flex-auto.shrink-0.gap-y-5.border.border-neutral-300.dark:border-neutral-700.rounded.p-5.z-20
   [:div.flex.justify-between.shrink-0
    [:h1.text-xl.font-semibold title]
    [:button {:on-click #(rf/dispatch [:modals/close])}
     [:i.fa-solid.fa-close]]]
   [:div.flex-auto.overflow-y-auto body]
   [:div.flex.justify-end.gap-x-3.shrink-0
    (if extra-buttons
      (map-indexed #(with-meta %2 {:key %1}) extra-buttons)
      [layout/primary-button "Ok" #(rf/dispatch [:modals/close])])]])

(defn modal-panel
  [{:keys [child]}]
  [:div.fixed.z-30.flex.flex-col.items-center.justify-center
   {:class ["top-1/2" "left-1/2" "-translate-x-1/2" "-translate-y-1/2"
            "w-4/5" "sm:w-3/4" "md:w-3/5" "lg:w-1/2" "xl:w-1/3"]}
   [:div.w-full.max-h-full
    child]])

(defn modals-container
  []
  (fn []
    (let [modals        @(rf/subscribe [:modals])
          visible-modal (last (filter :show? modals))]
      (when visible-modal
        [modal-panel visible-modal]))))
