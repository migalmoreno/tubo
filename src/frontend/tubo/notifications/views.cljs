(ns tubo.notifications.views
  (:require
   [re-frame.core :as rf]))

(defn notification-content
  [{:keys [failure parse-error status status-text] :as notification}]
  (when notification
    [:div.relative.flex.flex-col.justify-center.pl-4.pr-8.py-4.rounded.backdrop-blur.shadow.shadow-neutral-700
     {:class (case failure
               :success ["bg-green-600/90" "text-white"]
               :error   ["bg-red-600/90" "text-white"]
               ["bg-neutral-300"])}
     [:button.text-lg.absolute.top-1.right-2
      {:on-click
       #(rf/dispatch [:notifications/remove (:id notification)])}
      [:i.fa-solid.fa-close]]
     [:span.font-bold (str (when status (str status ": ")) status-text)]
     (when parse-error
       [:span.line-clamp-1 (:status-text parse-error)])]))

(defn notifications-panel
  []
  (fn []
    (let [notifications @(rf/subscribe [:notifications])]
      [:div.fixed.flex.flex-col.items-end.gap-2.top-16.z-20.w-full.py-1.px-2
       (for [[i notification] (map-indexed vector notifications)]
         ^{:key i} [notification-content notification])])))
