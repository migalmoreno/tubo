(ns tubo.components.notification
  (:require
   [re-frame.core :as rf]
   [tubo.events :as events]))

(defn notification-content
  [{:keys [failure parse-error status status-text] :as notification} key]
  (when notification
    [:div.py-4.pl-4.pr-8.rounded.backdrop-blur.flex.flex-col.justify-center.shadow.shadow-neutral-700
     {:class (clojure.string/join
              "" (case failure
                   :success ["bg-green-600/90 text-white"]
                   :error   ["bg-red-600/90 text-white"]
                   ["bg-neutral-300"]))}
     [:button.text-lg.absolute.top-1.right-2
      {:on-click #(rf/dispatch [::events/remove-notification key])}
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
         ^{:key i} [notification-content notification i])])))
