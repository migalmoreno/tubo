(ns tubo.search.views
  (:require
   [re-frame.core :as rf]
   [tubo.items.views :as items]
   [tubo.layout.views :as layout]))

(defn search
  [{{:keys [q serviceId]} :query-params}]
  (let [{:keys [items next-page]} @(rf/subscribe [:search/results])
        next-page-url             (:url next-page)
        service-id                (or @(rf/subscribe [:service-id]) serviceId)
        scrolled-to-bottom?       @(rf/subscribe [:scrolled-to-bottom])]
    (when scrolled-to-bottom?
      (rf/dispatch [:search/fetch-paginated q service-id next-page-url]))
    [layout/content-container
     [items/related-streams items next-page-url]]))
