(ns tubo.bookmarks.events
  (:require
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :bookmarks/add
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark notify?]]
   (let [updated-db (update db
                            :bookmarks
                            conj
                            (if (:id bookmark)
                              bookmark
                              (assoc bookmark :id (nano-id))))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    [[:dispatch [:modals/close]]
              (when notify?
                [:dispatch
                 [:notifications/add
                  {:status-text
                   (str "Added playlist \"" (:name bookmark) "\"")
                   :failure :success}]])]})))

(rf/reg-event-fx
 :bookmarks/remove
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ id notify?]]
   (let [bookmark   (first (filter #(= (:id %) id) (:bookmarks db)))
         updated-db (update db
                            :bookmarks
                            #(into []
                                   (remove (fn [bookmark]
                                             (= (:id bookmark) id))
                                           %)))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    (if notify?
               [[:dispatch
                 [:notifications/add
                  {:status-text
                   (str "Removed playlist \"" (:name bookmark) "\"")
                   :failure :success}]]]
               [])})))

(rf/reg-event-fx
 :bookmarks/clear
 (fn [{:keys [db]} _]
   {:fx (conj (into
               (map (fn [bookmark]
                      [:dispatch [:bookmarks/remove (:id bookmark)]])
                    (rest (:bookmarks db)))
               (map (fn [item]
                      [:dispatch [:likes/remove item]])
                    (:items (first (:bookmarks db)))))
              [:dispatch
               [:notifications/add
                {:status-text "Cleared all playlists"
                 :failure     :success}]])}))

(rf/reg-event-fx
 :likes/add-n
 (fn [_ [_ items notify?]]
   {:fx (conj (map (fn [item]
                     [:dispatch [:likes/add item]])
                   items)
              (when notify?
                [:dispatch
                 [:notifications/add
                  {:status-text (str "Added "
                                     (count items)
                                     " items to likes")
                   :failure     :success}]]))}))

(rf/reg-event-fx
 :likes/add
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ item notify?]]
   (when-not (some #(= (:url %) (:url item))
                   (-> db
                       :bookmarks
                       first
                       :items))
     (let [updated-db (update-in db
                                 [:bookmarks 0 :items]
                                 #(into [] (conj (into [] %1) %2))
                                 (assoc item
                                        :bookmark-id
                                        (-> db
                                            :bookmarks
                                            first
                                            :id)))]
       {:db    updated-db
        :store (assoc store :bookmarks (:bookmarks updated-db))
        :fx    (if notify?
                 [[:dispatch
                   [:notifications/add
                    {:status-text "Added to favorites"
                     :failure     :success}]]]
                 [])}))))

(rf/reg-event-fx
 :likes/remove
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ item notify?]]
   (let [updated-db (update-in db
                               [:bookmarks 0 :items]
                               (fn [items]
                                 (remove #(= (:url %) (:url item)) items)))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    (if notify?
               [[:dispatch
                 [:notifications/add
                  {:status-text "Removed from favorites"
                   :failure     :success}]]]
               [])})))

(rf/reg-event-fx
 :bookmark/add-n
 (fn [_ [_ bookmark items]]
   {:fx (conj (map (fn [item]
                     [:dispatch [:bookmark/add bookmark item]])
                   items)
              [:dispatch
               [:notifications/add
                {:status-text (str "Added "
                                   (count items)
                                   " items to playlist \""
                                   (:name bookmark)
                                   "\"")
                 :failure     :success}]])}))

(rf/reg-event-fx
 :bookmark/add
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark item notify?]]
   (let [selected   (first (filter #(= (:id %) (:id bookmark)) (:bookmarks db)))
         pos        (.indexOf (:bookmarks db) selected)
         updated-db (if (some #(= (:url %) (:url item)) (:items selected))
                      db
                      (update-in db
                                 [:bookmarks pos :items]
                                 #(into [] (conj (into [] %1) %2))
                                 (assoc item :bookmark-id (:id bookmark))))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    [[:dispatch [:modals/close]]
              (when notify?
                [:dispatch
                 [:notifications/add
                  {:status-text (str "Added to playlist \""
                                     (:name selected)
                                     "\"")
                   :failure     :success}]])]})))

(rf/reg-event-fx
 :bookmark/remove
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} [_ bookmark]]
   (let [selected   (first (filter #(= (:id %) (:bookmark-id bookmark))
                                   (:bookmarks db)))
         pos        (.indexOf (:bookmarks db) selected)
         updated-db (update-in db
                               [:bookmarks pos :items]
                               #(remove (fn [item]
                                          (= (:url item) (:url bookmark)))
                                        %))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    [[:dispatch
               [:notifications/add
                {:status-text (str "Removed from playlist \""
                                   (:name selected)
                                   "\"")
                 :failure     :success}]]]})))

(rf/reg-event-fx
 :bookmarks/add-imported
 (fn [_ [_ bookmarks]]
   {:fx (conj (map-indexed (fn [i bookmark]
                             (if (= i 0)
                               [:dispatch [:likes/add-n (:items bookmark)]]
                               [:dispatch [:bookmarks/add bookmark]]))
                           bookmarks)
              [:dispatch
               [:notifications/add
                {:status-text (str "Imported "
                                   (count bookmarks)
                                   " playlists successfully")
                 :failure     :success}]])}))

(defn fetch-imported-bookmarks-items
  [bookmarks]
  (-> #(-> (p/all (map (fn [stream]
                         (-> (js/fetch
                              (str "/api/v1/streams/"
                                   (js/encodeURIComponent stream)))
                             (p/then (fn [res] (.json res)))
                             (p/catch (fn []
                                        (rf/dispatch
                                         [:notifications/add
                                          {:status-text
                                           (str "Error importing " stream)
                                           :failure :error}])))))
                       (:items %)))
           (p/then (fn [results]
                     (assoc % :items (remove nil? results)))))
      (map bookmarks)
      p/all))

(rf/reg-event-fx
 :bookmarks/process-import
 (fn [_ [_ bookmarks]]
   {:promise
    {:call         #(-> (fetch-imported-bookmarks-items bookmarks)
                        (p/then (fn [res]
                                  (js->clj res :keywordize-keys true))))
     :on-success-n [[:notifications/clear]
                    [:bookmarks/add-imported]]}
    :fx [[:dispatch
          [:notifications/add
           {:status-text "Importing playlists"
            :failure     :loading}
           false]]]}))

(rf/reg-fx
 :bookmarks/import!
 (fn [file]
   (-> (.text file)
       (p/then
        #(let [res (js->clj (.parse js/JSON %) :keywordize-keys true)]
           (if (= (:format res) "Tubo")
             (rf/dispatch [:bookmarks/process-import (:playlists res)])
             (throw (js/Error. "Format not supported")))))
       (p/catch js/Error
         (fn [error]
           (rf/dispatch [:notifications/add
                         {:status-text (.-message error)
                          :failure     :error}]))))))

(rf/reg-event-fx
 :bookmarks/import
 (fn [_ [_ files]]
   {:fx (map (fn [file] [:bookmarks/import! file]) files)}))

(rf/reg-event-fx
 :bookmarks/export
 (fn [{:keys [db]} [_]]
   {:file-download
    {:name      "playlists.json"
     :mime-type "application/json"
     :data      (.stringify
                 js/JSON
                 (clj->js {:format "Tubo"
                           :version 1
                           :playlists
                           (map (fn [bookmark]
                                  {:name  (:name bookmark)
                                   :items (map :url (:items bookmark))})
                                (:bookmarks db))}))}
    :fx [[:dispatch
          [:notifications/add
           {:status-text "Exported playlists"
            :failure     :success}]]]}))

(rf/reg-event-fx
 :bookmarks/fetch-page
 (fn [_]
   {:document-title "Bookmarked Playlists"}))

(rf/reg-event-fx
 :bookmark/fetch-page
 (fn [{:keys [db]} [_ playlist-id]]
   (let [playlist (first (filter #(= (:id %) playlist-id) (:bookmarks db)))]
     {:document-title (:name playlist)})))
