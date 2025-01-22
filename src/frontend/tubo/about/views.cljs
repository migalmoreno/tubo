(ns tubo.about.views
  (:require
   [reitit.frontend.easy :as rfe]
   [tubo.layout.views :as layout]
   [tubo.utils :refer [version]]))

(defn about-entry
  [title text & children]
  [:div.flex.flex-col.w-full.gap-y-6
   [:div.flex.flex-col.gap-y-2
    [:h4.font-semibold.text-xl title]
    [:span.text-neutral-600.dark:text-neutral-400 text]]
   [:div.flex
    (map-indexed #(with-meta %2 {:key %1}) children)]])

(defn about
  []
  [layout/content-container
   [:div
    [:div.my-10.flex.flex-col.items-center.gap-y-4
     [layout/logo :height 100 :width 100]
     [:div.flex.flex-col.items-center.gap-y-2
      [:h3.font-semibold.text-3xl "Tubo"]
      [:span.text-sm version]]
     [:div.text-neutral-600.dark:text-neutral-400
      "A libre streaming front-end for the web"]]
    [:div.flex.flex-col.gap-y-4.py-8.w-full
     [about-entry "Website"
      "Visit Tubo's homepage for more information and documentation."
      [:div.w-full.flex.justify-end
       [:a
        {:href   "https://migalmoreno.com/projects/tubo.html"
         :target "blank"
         :rel    "noopener"}
        [layout/secondary-button "Visit homepage"]]]]
     [about-entry "Contribute"
      "Feature requests, bug reports, and design ideas should be submitted in the upstream source repository."
      [:div.w-full.flex.justify-end
       [:a
        {:href   "https://github.com/migalmoreno/tubo"
         :target "blank"
         :rel    "noopener"}
        [layout/secondary-button "View on GitHub"]]]]
     [about-entry "Tubo's Privacy Policy"
      "Tubo takes your data very seriously. Therefore, the application does not collect any data without your consent."
      [:div.w-full.flex.justify-end
       [:a {:href (rfe/href :privacy-page)}
        [layout/secondary-button "Read Privacy Policy"]]]]]]])

(defn privacy-policy
  []
  [layout/content-container
   [layout/content-header "Tubo's Privacy Policy"]
   [:div.pt-4.flex.flex-col.gap-y-6
    [:p
     "Tubo takes your privacy very seriously. This privacy policy explains the type of information that is collected and recorded and how it is used."]
    [:h4.font-bold.text-xl "Local Data"]
    [:p
     "Tubo uses local browser storage to store user preferences and user data without an account. This data doesn't contain any identifying information."]
    [:p
     "You can remove this data by using your browser's cookie-related controls."]
    [:h4.font-bold.text-xl "Log Files"]
    [:p
     "Public instances hosting Tubo usually follow a standard procedure of using log files. The information collected by these may include:"]
    [:ul.list-disc.px-4
     [:li "The visitor's IP address"]
     [:li "The time the request was made"]
     [:li "The status code of the response"]
     [:li "The method of the request"]
     [:li "The device user agent of the request"]
     [:li "The requested URL"]
     [:li "How long it took to complete the request"]]
    [:h4.font-bold.text-xl "Third Party Privacy Policies"]
    [:p
     "Tubo's privacy policy does not apply to external platforms from which it might extract data. You are advised to consult their respective privacy policies for more detailed information."]]])
