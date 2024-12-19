(ns tubo.modals.views
  (:require
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]))

(defn modal-content
  [title body & extra-buttons]
  [:div.bg-neutral-100.dark:bg-neutral-900.flex.flex-col.flex-auto.gap-y-5.rounded.p-5
   [:div.flex.justify-between
    [:h1.text-xl.font-semibold title]
    [:button {:on-click #(rf/dispatch [:modals/close])}
     [:i.fa-solid.fa-close]]]
   [:div.flex-auto.overflow-y-auto body]
   [:div.flex.justify-end.gap-x-3
    (if extra-buttons
      (map-indexed #(with-meta %2 {:key %1}) extra-buttons)
      [layout/primary-button "Ok" #(rf/dispatch [:modals/close])])]])

(defn modal-panel
  [{:keys [child]}]
  [:div.flex.fixed.z-30
   {:class ["top-1/2" "left-1/2" "-translate-x-1/2" "-translate-y-1/2"
            "w-11/12" "sm:w-3/4" "md:w-3/5" "lg:w-1/2" "xl:w-1/3"
            "max-h-[90dvh]"]}
   child])

(defn modals-container
  []
  (let [modals        @(rf/subscribe [:modals])
        visible-modal (last (filter :show? modals))]
    (when visible-modal
      [modal-panel visible-modal])))
