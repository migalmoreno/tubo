(ns tau.views.stream
  (:require
   [re-frame.core :as rf]))

(defn stream
  [m]
  (let [current-stream @(rf/subscribe [:stream])
        stream-type (-> (if (empty? (:video-streams current-stream))
                          (:audio-streams current-stream)
                          (:video-streams current-stream))
                        last
                        :content)]
       [:div.flex.flex-col.justify-center.p-5.items-center
        [:div.flex.justify-center.py-2
         [:div.flex.justify-center {:class "w-4/5"}
          [:video.min-w-full.h-auto {:src stream-type :controls true}]]]
        [:div.flex.text-white
         [:button.border.rounded.border-slate-900.p-2.bg-slate-800
          {:on-click #(rf/dispatch [:switch-to-global-player current-stream])}
          "Add to global stream"]
         [:a {:href (:url current-stream)}
          "Open original source"]]
        [:div.flex.flex-col.items-center.py-2 {:class "w-4/5"}
         [:div.min-w-full.py-2
          [:h1.text-xl.font-extrabold (:name current-stream)]]
         [:div.min-w-full.py-2
          [:p (:description current-stream)]]]]))
