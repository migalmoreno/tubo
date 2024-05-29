(ns tubo.services.views
  (:require
   [re-frame.core :as rf]))

(defn services-dropdown
  [services service-id service-color]
  [:div.relative.flex.flex-col.items-center-justify-center.text-white.px-2
   {:style {:background service-color}}
   [:div.w-full.box-border.z-10.lg:z-0
    [:select.border-none.focus:ring-transparent.bg-blend-color-dodge.font-bold.w-full
     {:on-change #(rf/dispatch [:kiosks/change-page (js/parseInt (.. % -target -value))])
      :value     service-id
      :style     {:background :transparent}}
     (when services
       (for [[i service] (map-indexed vector services)]
         ^{:key i} [:option.text-white.bg-neutral-900.border-none
                    {:value (:id service)}
                    (-> service :info :name)]))]]
   [:div.flex.items-center.justify-end.absolute.min-h-full.top-0.right-4.lg:right-0.z-0
    [:i.fa-solid.fa-caret-down]]])
