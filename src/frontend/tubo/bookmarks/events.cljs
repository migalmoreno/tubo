(ns tubo.bookmarks.events
  (:require
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]))

(defn get-stream-metadata
  [stream]
  (select-keys stream
               [:type :service-id :url :name :thumbnails :verified?
                :uploader-name :uploader-url :uploader-avatars :upload-date
                :short-description :duration :view-count :uploaded]))

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
                 [:notifications/success
                  (str "Added playlist \"" (:name bookmark) "\"")]])]})))

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
                 [:notifications/success
                  (str "Removed playlist \"" (:name bookmark) "\"")]]]
               [])})))

(rf/reg-event-fx
 :bookmarks/clear
 (fn [{:keys [db]}]
   {:fx (conj (into
               (map (fn [bookmark]
                      [:dispatch [:bookmarks/remove (:id bookmark)]])
                    (rest (:bookmarks db)))
               (map (fn [item]
                      [:dispatch [:likes/remove item]])
                    (:items (first (:bookmarks db)))))
              [:dispatch
               [:notifications/success "Cleared all playlists"]])}))

(rf/reg-event-fx
 :likes/add-n
 (fn [_ [_ items notify?]]
   {:fx (conj (map (fn [item]
                     [:dispatch [:likes/add item]])
                   items)
              (when notify?
                [:dispatch
                 [:notifications/suuccess
                  (str "Added " (count items) " items to likes")]]))}))

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
                                 (assoc (get-stream-metadata item)
                                        :bookmark-id
                                        (-> db
                                            :bookmarks
                                            first
                                            :id)))]
       {:db    updated-db
        :store (assoc store :bookmarks (:bookmarks updated-db))
        :fx    (if notify?
                 [[:dispatch [:notifications/success "Added to favorites"]]]
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
                 [:notifications/success "Removed from favorites"]]]
               [])})))

(rf/reg-event-fx
 :bookmark/add-n
 (fn [_ [_ bookmark items notify?]]
   {:fx (conj (map (fn [item]
                     [:dispatch [:bookmark/add bookmark item]])
                   items)
              (when notify?
                [:dispatch
                 [:notifications/success
                  (str "Added "
                       (count items)
                       " items to playlist \""
                       (:name bookmark)
                       "\"")]]))}))

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
                                 (assoc (get-stream-metadata item)
                                        :bookmark-id
                                        (:id bookmark))))]
     {:db    updated-db
      :store (assoc store :bookmarks (:bookmarks updated-db))
      :fx    [[:dispatch [:modals/close]]
              (when notify?
                [:dispatch
                 [:notifications/success
                  (str "Added to playlist \"" (:name selected) "\"")]])]})))

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
               [:notifications/success
                (str "Removed from playlist \"" (:name selected) "\"")]]]})))

(rf/reg-event-fx
 :bookmarks/add-imported
 (fn [_ [_ bookmarks]]
   {:fx (conj
         (map-indexed (fn [i bookmark]
                        (if (= i 0)
                          [:dispatch [:likes/add-n (:items bookmark)]]
                          [:dispatch [:bookmarks/add bookmark]]))
                      bookmarks)
         [:dispatch
          [:notifications/success
           (str "Imported " (count bookmarks) " playlists successfully")]])}))

(defn fetch-imported-bookmarks-items
  [bookmarks]
  (-> #(-> (p/all
            (map (fn [stream]
                   (-> (js/fetch
                        (str "/api/v1/streams/"
                             (js/encodeURIComponent stream)))
                       (p/then (fn [res] (.json res)))
                       (p/catch (fn []
                                  (rf/dispatch
                                   [:notifications/error
                                    (str "Error importing " stream)])))))
                 (:items %)))
           (p/then (fn [results]
                     (assoc %
                            :items
                            (map (fn [item]
                                   (get-stream-metadata
                                    (js->clj item :keywordize-keys true)))
                                 (remove nil? results))))))
      (map bookmarks)
      p/all))

(rf/reg-event-fx
 :bookmarks/process-import
 (fn [_ [_ bookmarks]]
   {:promise
    {:call         #(fetch-imported-bookmarks-items bookmarks)
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
           (rf/dispatch [:notifications/error (.-message error)]))))))

(rf/reg-event-fx
 :bookmarks/import
 (fn [_ [_ files]]
   {:fx (map (fn [file] [:bookmarks/import! file]) files)}))

(rf/reg-event-fx
 :bookmarks/export
 (fn [{:keys [db]}]
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
          [:notifications/success "Exported playlists"]]]}))

(rf/reg-event-fx
 :bookmarks/fetch-page
 (fn [_]
   {:document-title "Bookmarked Playlists"}))

(rf/reg-event-fx
 :bookmark/fetch-page
 (fn [{:keys [db]} [_ playlist-id]]
   (let [playlist (first (filter #(= (:id %) playlist-id) (:bookmarks db)))]
     {:document-title (:name playlist)})))
