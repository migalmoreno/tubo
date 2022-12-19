(ns tau.views.home)

(defn home-page
  []
  [:div.flex.justify-center.content-center.flex-col.text-center.text-white.text-lg.flex-auto
   [:p.text-5xl.p-5 "Welcome to Tau"]
   [:p.text-2xl "A web front-end for Newpipe"]])
