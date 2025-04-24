(ns tubo.models.user
  (:require
   [buddy.hashers :as bh]
   [honey.sql :as sql]
   [nano-id.core :refer [nano-id]]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn get-user-by-username
  [username datasource]
  (jdbc/execute-one! datasource
                     (-> {:select [:*]
                          :from   [:users]
                          :where  [:= :username username]}
                         sql/format)
                     {:builder-fn rs/as-unqualified-kebab-maps}))

(defn create-user
  [{:keys [username password]} datasource]
  (when-let [user (jdbc/execute-one!
                   datasource
                   (-> {:insert-into :users
                        :columns     [:username :password :session_id]
                        :values      [[username (bh/derive password)
                                       (nano-id 32)]]}
                       sql/format)
                   {:return-keys true
                    :builder-fn  rs/as-unqualified-kebab-maps})]
    (dissoc user :password)))

(defn get-user-by-session
  [session-id datasource]
  (when-let [user (jdbc/execute-one! datasource
                                     (-> {:select [:*]
                                          :from   [:users]
                                          :where  [:= :session_id
                                                   (str session-id)]}
                                         sql/format)
                                     {:builder-fn rs/as-kebab-maps})]
    {:id         (:users/id user)
     :username   (:users/username user)
     :created-at (:users/created-at user)
     :session-id (:users/session-id user)}))

(defn invalidate-user-session-id
  [id datasource]
  (jdbc/execute-one! datasource
                     (-> {:update [:users]
                          :set    [:session_id (nano-id 32)]
                          :where  [:= :session_id id]}
                         sql/format)))
