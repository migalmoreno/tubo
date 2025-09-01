(ns tubo.bookmarks.events
  (:require
   [nano-id.core :refer [nano-id]]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [fork.re-frame :as fork]
   [malli.core :as m]
   [malli.error :as me]
   [tubo.layout.events :as le]
   [tubo.schemas :as s]
   [tubo.storage :refer [persist]]
   [tubo.utils :as utils]))

(defn apply-playlist-stream-transforms
  [db item]
  (-> item
      (utils/apply-image-quality db :thumbnail :thumbnails)
      (utils/apply-image-quality db
                                 :uploader-avatar
                                 :uploader-avatars)
      (select-keys
       [:type :service-id :url :name :thumbnail :verified?
        :uploader-name :uploader-url :uploader-avatar :uploader-verified?
        :upload-date :short-description :duration :view-count :uploaded])))

(defn apply-auth-playlist-stream-transforms
  [db item]
  (-> (apply-playlist-stream-transforms db item)
      (select-keys [:url :name :thumbnail :duration :uploader-avatar
                    :uploader-url :uploader-verified? :uploader-name])))

(rf/reg-event-fx
 :bookmarks/on-add-auth
 (fn [{:keys [db]} [_ notify? path {:keys [body]}]]
   (let [updated-db
         (update db :user/bookmarks #(into [] (conj (into [] %1) %2)) body)]
     {:db (if path (fork/set-submitting updated-db path false) updated-db)
      :fx [(when notify?
             [:dispatch
              [:notifications/success
               (str "Added playlist \"" (:name body) "\"")]])]})))

(rf/reg-event-fx
 :bookmarks/add
 [persist]
 (fn [{:keys [db]} [_ bookmark notify? path]]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/post-auth "user/playlists"
             (update bookmark
                     :items
                     (fn [items]
                       (map #(apply-auth-playlist-stream-transforms db %)
                            items)))
             [:bookmarks/on-add-auth notify? path]
             (if path [:on-form-submit-failure] [:bad-response])]]]}
     (let [updated-db (update db
                              :bookmarks
                              conj
                              (if (:id bookmark)
                                bookmark
                                (assoc bookmark :id (nano-id))))]
       {:db updated-db
        :fx [(when notify?
               [:dispatch
                [:notifications/success
                 (str "Added playlist \"" (:name bookmark) "\"")]])]}))))

(rf/reg-event-fx
 :bookmarks/handle-add-form
 (fn [{:keys [db]} [_ notify? {:keys [path values]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch [:modals/close]]
         [:dispatch [:bookmarks/add values notify? path]]]}))

(rf/reg-event-fx
 :bookmarks/on-delete-auth
 (fn [{:keys [db]} [_ id notify? {:keys [body]}]]
   {:db (update db
                :user/bookmarks
                #(into []
                       (remove (fn [bookmark]
                                 (= (:playlist-id bookmark) id))
                               %)))
    :fx [[:dispatch [:modals/close]]
         (if notify?
           [:dispatch
            [:notifications/success
             (str "Removed playlist \"" (:name body) "\"")]]
           [])]}))

(rf/reg-event-fx
 :bookmarks/remove
 [persist]
 (fn [{:keys [db]} [_ id notify?]]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/delete-auth (str "user/playlists/" id)
             [:bookmarks/on-delete-auth id notify?] [:bad-response]]]]}
     (let [bookmark   (first (filter #(= (:id %) id) (:bookmarks db)))
           updated-db (update db
                              :bookmarks
                              #(into []
                                     (remove (fn [bookmark]
                                               (= (:id bookmark) id))
                                             %)))]
       {:db updated-db
        :fx (if notify?
              [[:dispatch
                [:notifications/success
                 (str "Removed playlist \"" (:name bookmark) "\"")]]]
              [])}))))

(rf/reg-event-fx
 :bookmarks/on-clear-auth
 (fn [{:keys [db]}]
   {:db (assoc db :user/bookmarks nil)
    :fx [[:dispatch [:notifications/success "Cleared all playlists"]]]}))

(rf/reg-event-fx
 :bookmarks/clear
 (fn [{:keys [db]}]
   (if (:auth/user db)
     {:fx [[:dispatch
            [:api/delete-auth "user/playlists"
             [:bookmarks/on-clear-auth] [:bad-response]]]]}
     {:fx (conj (into
                 (map (fn [bookmark]
                        [:dispatch
                         [:bookmarks/remove
                          (or (:playlist-id bookmark) (:id bookmark))]])
                      (:bookmarks db)))
                [:dispatch
                 [:notifications/success "Cleared all playlists"]])})))

(rf/reg-event-fx
 :bookmark/on-add-auth
 (fn [{:keys [db]} [_ playlist notify? {:keys [body]}]]
   (let [updated-db (update
                     db
                     :user/bookmarks
                     (fn [bookmarks]
                       (map
                        (fn [bookmark]
                          (if (= (:playlist-id bookmark)
                                 (:playlist-id playlist))
                            (update bookmark
                                    :items
                                    #(into (into [] %1) (into [] %2))
                                    body)
                            bookmark))
                        bookmarks)))]
     {:db updated-db
      :fx [[:dispatch [:modals/close]]
           (when notify?
             [:dispatch
              (if (= (count body) 0)
                [:notifications/error "Could not add to playlist"]
                [:notifications/success
                 (str "Added"
                      (when (> (count body) 1)
                        (str " " (count body) " items"))
                      " to playlist \""
                      (:name playlist)
                      "\"")])])]})))

(rf/reg-event-fx
 :bookmark/add-n
 (fn [{:keys [db]} [_ bookmark items notify?]]
   {:fx (if (:auth/user db)
          [[:dispatch
            [:api/post-auth
             (str "user/playlists/" (:playlist-id bookmark) "/add-streams")
             (map #(apply-auth-playlist-stream-transforms db %) items)
             [:bookmark/on-add-auth bookmark true] [:bad-response]]]]
          (conj (map (fn [item]
                       [:dispatch [:bookmark/add bookmark item]])
                     items)
                (when notify?
                  [:dispatch
                   [:notifications/success
                    (str "Added "
                         (count items)
                         " items to playlist \""
                         (:name bookmark)
                         "\"")]])))}))

(rf/reg-event-fx
 :bookmark/add
 [persist]
 (fn [{:keys [db]} [_ bookmark item notify?]]
   (if (:auth/user db)
     {:fx
      [[:dispatch
        [:api/post-auth
         (str "user/playlists/" (:playlist-id bookmark) "/add-streams")
         [(apply-auth-playlist-stream-transforms db item)]
         [:bookmark/on-add-auth bookmark notify?] [:bad-response]]]]}
     (let [selected   (first (filter #(= (:id %) (:id bookmark))
                                     (:bookmarks db)))
           pos        (.indexOf (:bookmarks db) selected)
           updated-db (if (some #(= (:url %) (:url item)) (:items selected))
                        db
                        (update-in db
                                   [:bookmarks pos :items]
                                   #(into [] (conj (into [] %1) %2))
                                   (assoc
                                    (apply-playlist-stream-transforms db item)
                                    :playlist-id
                                    (:id bookmark))))]
       {:db updated-db
        :fx [[:dispatch [:modals/close]]
             (when notify?
               [:dispatch
                [:notifications/success
                 (str "Added to playlist \"" (:name selected) "\"")]])]}))))

(rf/reg-event-fx
 :bookmark/on-remove-streams-auth
 (fn [{:keys [db]} [_ playlist {:keys [body]}]]
   (let [updated-db (update
                     db
                     :user/bookmarks
                     (fn [bookmarks]
                       (map (fn [bookmark]
                              (if (= (:playlist-id bookmark)
                                     (:playlist-id playlist))
                                (update bookmark
                                        :items
                                        (fn [items]
                                          (remove #(= (:url %) (:url body))
                                                  items)))
                                bookmark))
                            bookmarks)))
         selected   (first (filter #(= (:playlist-id %) (:playlist-id playlist))
                                   (:user/bookmarks updated-db)))]
     {:db updated-db
      :fx [[:dispatch
            [:notifications/success
             (str "Removed from playlist \"" (:name selected) "\"")]]]})))

(rf/reg-event-fx
 :bookmark/remove
 [persist]
 (fn [{:keys [db]} [_ bookmark]]
   (if (:auth/user db)
     (let [playlist (first (filter #(= (:playlist-id %) (:playlist-id bookmark))
                                   (:user/bookmarks db)))]
       {:fx [[:dispatch
              [:api/post-auth
               (str "user/playlists/" (:playlist-id bookmark) "/delete-stream")
               (apply-auth-playlist-stream-transforms db bookmark)
               [:bookmark/on-remove-streams-auth playlist]
               [:bad-response]]]]})
     (let [selected   (first (filter #(= (:id %) (:playlist-id bookmark))
                                     (:bookmarks db)))
           pos        (.indexOf (:bookmarks db) selected)
           updated-db (update-in db
                                 [:bookmarks pos :items]
                                 #(remove (fn [item]
                                            (= (:url item) (:url bookmark)))
                                          %))]
       {:db updated-db
        :fx [[:dispatch
              [:notifications/success
               (str "Removed from playlist \""
                    (:name selected)
                    "\"")]]]}))))

(rf/reg-event-fx
 :bookmarks/add-imported
 (fn [_ [_ bookmarks]]
   {:fx (conj
         (map (fn [bookmark] [:dispatch [:bookmarks/add bookmark]]) bookmarks)
         [:dispatch
          [:notifications/success
           (str "Imported " (count bookmarks) " playlists successfully")]])}))

(defn fetch-imported-bookmarks-items
  [db bookmarks]
  (-> (fn [bookmark]
        (-> (p/all
             (map (fn [stream]
                    (-> (js/fetch
                         (str (get-in db [:settings :instance])
                              "/api/v1/streams/"
                              (js/encodeURIComponent stream)))
                        (p/then (fn [res]
                                  (if (= (.-status res) 200)
                                    (.json res)
                                    (throw (js/Error. (str "Error importing "
                                                           stream))))))
                        (p/catch (fn [error]
                                   (rf/dispatch
                                    [:notifications/error
                                     (.-message error)])))))
                  (:items bookmark)))
            (p/then (fn [results]
                      (if (= (count (remove nil? results)) 0)
                        (throw (js/Error. (str "Error importing playlist \""
                                               (:name bookmark)
                                               "\"")))
                        (assoc bookmark
                               :items
                               (map
                                (fn [item]
                                  (->> (js->clj item :keywordize-keys true)
                                       (apply-playlist-stream-transforms db)))
                                (remove nil? results))))))
            (p/catch (fn [error]
                       (rf/dispatch
                        [:notifications/error (.-message error)])))))
      (map bookmarks)
      p/all
      (p/then (fn [bookmarks]
                (if (= (count (remove nil? bookmarks)) 0)
                  (throw (js/Error. "There was a problem importing playlists"))
                  bookmarks)))))

(rf/reg-event-fx
 :bookmarks/handle-import-failure
 (fn [_ [_ error]]
   {:fx [[:dispatch [:notifications/error (.-message error)]]]}))

(rf/reg-event-fx
 :bookmarks/process-import
 (fn [{:keys [db]} [_ bookmarks]]
   {:promise
    {:call         #(fetch-imported-bookmarks-items db bookmarks)
     :on-success-n [[:notifications/clear]
                    [:bookmarks/add-imported]]
     :on-failure-n [[:notifications/clear]
                    [:bookmarks/handle-import-failure]]}
    :fx [[:dispatch
          [:notifications/add
           {:status-text "Importing playlists"
            :type        :loading}
           false]]]}))

(rf/reg-fx
 :bookmarks/import!
 (fn [file]
   (-> (.text file)
       (p/then
        #(let [res (js->clj (.parse js/JSON %) :keywordize-keys true)]
           (if-let [error (me/humanize (m/explain s/PlaylistsConfigFile res))]
             (throw (js/Error. (str (name (first (keys error)))
                                    ": "
                                    (first (vals error)))))
             (rf/dispatch [:bookmarks/process-import (:playlists res)]))))
       (p/catch js/Error
         (fn [error]
           (rf/dispatch [:notifications/error (.-message error)]))))))

(rf/reg-event-fx
 :bookmarks/import
 (fn [_ [_ event]]
   (let [tooltip-id (le/find-clicked-controller-id (.-target event))]
     {:fx (into (map (fn [file] [:bookmarks/import! file])
                     (.. event -target -files))
                [[:dispatch [:layout/destroy-tooltip-by-id tooltip-id]]
                 [:dispatch [:layout/hide-bg-overlay]]])})))

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
                                  {:name      (:name bookmark)
                                   :thumbnail (:thumbnail bookmark)
                                   :items     (map :url (:items bookmark))})
                                (get db
                                     (if (:auth/user db)
                                       :user/bookmarks
                                       :bookmarks)))}))}
    :fx [[:dispatch
          [:notifications/success "Exported playlists"]]]}))

(rf/reg-event-fx
 :bookmarks/load-authenticated-playlists
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db
               :user/bookmarks
               (into []
                     (map
                      #(utils/apply-image-quality % db :thumbnail :thumbnail)
                      body)))}))

(rf/reg-event-fx
 :bookmarks/fetch-page
 (fn [{:keys [db]}]
   {:document-title (if (:auth/user db)
                      (str (:username (:auth/user db)) "'s playlists")
                      "Bookmarked playlists")
    :fx             [[:dispatch [:bookmarks/fetch-authenticated-playlists]]]}))

(rf/reg-event-fx
 :bookmark/load-authenticated-playlist
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:document-title (:name body)
    :db
    (->
      (update
       db
       :user/bookmarks
       (fn [bookmarks]
         (map
          #(if (= (:playlist-id %) (:playlist-id body))
             (utils/apply-image-quality-n body db :items :thumbnail :thumbnail)
             %)
          bookmarks)))
      (assoc :show-page-loading false))}))

(rf/reg-event-fx
 :bookmarks/on-fetch-authenticated-playlists
 (fn [_ [_ cb res]]
   {:fx [[:dispatch [:bookmarks/load-authenticated-playlists res]]
         (when cb
           [:dispatch cb])]}))

(rf/reg-event-fx
 :bookmarks/fetch-authenticated-playlists
 (fn [{:keys [db]} [_ cb]]
   {:fx (if (:auth/user db)
          [[:dispatch
            [:api/get-auth "user/playlists"
             [:bookmarks/on-fetch-authenticated-playlists cb]
             [:bad-page-response [:auth/redirect-login]]]]]
          [])}))

(rf/reg-event-fx
 :bookmark/fetch-page
 (fn [{:keys [db]} [_ playlist-id]]
   (if (:auth/user db)
     {:db (assoc db :show-page-loading true)
      :fx [[:dispatch
            [:api/get-auth (str "user/playlists/" playlist-id)
             [:bookmark/load-authenticated-playlist]
             [:bad-page-response [:auth/redirect-login]]]]]}
     (let [playlist (first (filter #(= (:id %) playlist-id) (:bookmarks db)))]
       {:document-title (:name playlist)}))))

(rf/reg-event-fx
 :bookmark/on-update-info-auth-success
 (fn [{:keys [db]} [_ path {:keys [body]}]]
   {:db (-> (fork/set-submitting db path false)
            (update :user/bookmarks
                    (fn [bookmarks]
                      (map #(if (= (:playlist-id %) (:playlist-id body)) body %)
                           bookmarks))))
    :fx [[:dispatch [:modals/close]]
         [:dispatch
          [:notifications/success
           (str "Updated playlist \"" (:name body) "\"")]]]}))

(rf/reg-event-fx
 :bookmark/edit
 (fn [{:keys [db]} [_ playlist {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :fx [[:dispatch
          [:api/put-auth (str "user/playlists/" (:playlist-id playlist))
           values
           [:bookmark/on-update-info-auth-success path]
           [:on-form-submit-failure path]]]]}))
