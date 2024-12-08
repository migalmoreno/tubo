(ns tubo.notifications.views
  (:require
   [re-frame.core :as rf]
   [tubo.layout.views :as layout]))

(defn notification-content
  [{:keys [failure parse-error status status-text] :as notification}]
  (when notification
    [:div.relative.flex.justify-center.pl-4.pr-8.py-5.rounded.shadow.dark:shadow-neutral-900.shadow-neutral-400
     {:class (case failure
               :success ["bg-green-600/90" :text-white]
               :error   ["bg-red-600/90" :text-white]
               ["dark:bg-neutral-800" "dark:text-white" :bg-neutral-100
                :text-neutral-800])}
     [:div.flex.items-center.gap-x-4
      (case failure
        :success [:i.fa-solid.fa-circle-check]
        :error   [:i.fa-solid.fa-circle-exclamation]
        :loading [:div.grow-0 [layout/loading-icon]]
        [:i.fa-solid.fa-circle-info])
      [:div.flex.flex-col
       [:button.absolute.top-1.right-2
        {:on-click
         #(rf/dispatch [:notifications/remove (:id notification)])}
        [:i.fa-solid.fa-close]]
       [:span.font-bold.break-all
        (str status " " status-text)]
       (when parse-error
         [:span.line-clamp-1
          (or (:original-text parse-error) (:status-text parse-error))])]]]))

(defn notifications-panel
  []
  (fn []
    (let [notifications @(rf/subscribe [:notifications])]
      [:div.fixed.flex.flex-col.items-end.gap-2.top-16.z-20.w-full.py-1.px-2
       (for [[i notification] (map-indexed vector notifications)]
         ^{:key i} [notification-content notification])])))
