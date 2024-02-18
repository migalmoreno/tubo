(ns tubo.components.modal
  (:require
   [re-frame.core :as rf]
   [tubo.components.layout :as layout]))

(defn modal-content
  [title body & extra-buttons]
  [:div.bg-white.max-h-full.dark:bg-neutral-900.z-20.p-5.rounded.flex.gap-y-5.flex-col.border.border-neutral-300.dark:border-stone-700.flex-auto.shrink-0
   [:div.flex.justify-between.shrink-0
    [:h1.text-xl.font-nunito-semibold title]
    [:button {:on-click #(rf/dispatch [:tubo.events/close-modal])}
     [:i.fa-solid.fa-close]]]
   [:div.flex-auto.overflow-y-auto body]
   [:div.flex.justify-end.gap-x-3.shrink-0
    (if extra-buttons
      (map-indexed #(with-meta %2 {:key %1}) extra-buttons)
      [layout/primary-button "Ok" #(rf/dispatch [:tubo.events/close-modal])])]])

(defn modal-panel
  [{:keys [child show?]}]
  [:div.fixed.flex.flex-col.items-center.justify-center.w-full.z-20.top-0
   {:style {:minHeight "100dvh" :height "100dvh"}}
   [layout/focus-overlay #(rf/dispatch [:tubo.events/close-modal]) show?]
   [:div.flex.items-center.justify-center.max-h-full.flex-auto.shrink-0.p-5
    {:class "w-full sm:w-3/4 md:w-3/5 lg:w-1/2 xl:w-1/3"}
    child]])

(defn modal
  []
  (fn []
    (let [modal (rf/subscribe [:modal])]
      (when (:show? @modal)
        [modal-panel @modal]))))
