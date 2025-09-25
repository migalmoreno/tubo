(ns tubo.modals.views
  (:require
   ["motion/react" :refer [motion AnimatePresence]]
   [re-frame.core :as rf]))

(defn modal-content
  [title body & extra-buttons]
  [:div.bg-neutral-100.dark:bg-neutral-950.flex.flex-col.flex-auto.gap-y-5.rounded.p-5.border.dark:border-neutral-900
   [:div.flex.justify-between
    [:h1.text-xl.font-extrabold title]
    [:button {:on-click #(rf/dispatch [:modals/close])}
     [:i.fa-solid.fa-close.text-neutral-600.dark:text-neutral-400.text-xl]]]
   [:div.flex-auto.overflow-y-auto body]
   (when extra-buttons
     [:div.flex.justify-end.gap-x-3
      (map-indexed #(with-meta %2 {:key %1}) extra-buttons)])])

(defn modal-panel
  [{:keys [child]}]
  [:> (.-div motion)
   {:class      ["flex" "fixed" "z-30" "top-1/2" "w-[90%]" "align-middle"
                 "left-1/2" "w-11/12" "sm:w-3/4" "md:w-3/5" "lg:w-1/2"
                 "xl:w-1/3"
                 "max-h-[90dvh]"]
    :style      {:translate "-50% -50%"}
    :animate    {:scale 1}
    :initial    {:scale 0}
    :exit       {:scale 0}
    :transition {:ease "easeInOut" :duration 0.5 :type "spring" :bounce 0.2}}
   child])

(defn modals-container
  []
  (let [modals                         @(rf/subscribe [:modals])
        {:keys [id] :as visible-modal} (last (filter :show? modals))]
    [:> AnimatePresence
     (when visible-modal
       ^{:key id} [modal-panel visible-modal])]))
