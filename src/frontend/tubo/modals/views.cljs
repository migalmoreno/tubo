(ns tubo.modals.views
  (:require
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]))

(defn modal-content
  [title body & extra-buttons]
  [:div.bg-white.dark:bg-neutral-900.max-h-full.flex.flex-col.flex-auto.shrink-0.gap-y-5.border.border-neutral-300.dark:border-stone-700.rounded.p-5.z-20
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
  [{:keys [child show?]}]
  [:div.fixed.flex.flex-col.items-center.justify-center.w-full.z-20.top-0
   {:class ["min-h-[100dvh]" "h-[100dvh]"]}
   [layout/focus-overlay #(rf/dispatch [:modals/close]) show?]
   [:div.flex.items-center.justify-center.flex-auto.shrink-0.w-full.max-h-full.p-5
    {:class ["sm:w-3/4" "md:w-3/5" "lg:w-1/2" "xl:w-1/3"]}
    child]])

(defn modal
  []
  (fn []
    (let [modals        @(rf/subscribe [:modals])
          visible-modal (last (filter :show? modals))]
      (when visible-modal
        [modal-panel visible-modal]))))
