(ns tubo.models.user
  (:require
   [buddy.hashers :as bh]
   [nano-id.core :refer [nano-id]]
   [tubo.db :as db]))

(defn get-user-by-username
  [username ds]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:users]
                    :where  [:= :username username]}))

(defn create-user
  [{:keys [username password]} ds]
  (when-let [user (db/execute-one! ds
                                   {:insert-into :users
                                    :columns     [:username :password
                                                  :session_id]
                                    :values      [[username (bh/derive password)
                                                   (nano-id 36)]]})]
    (dissoc user :password)))

(defn get-user-by-session
  [session-id datasource]
  (db/execute-one! datasource
                   {:select [:*]
                    :from   [:users]
                    :where  [:= :session_id (str session-id)]}))

(defn invalidate-user-session-id
  [id datasource]
  (db/execute-one! datasource
                   {:update [:users]
                    :set    {:session_id (nano-id 36)}
                    :where  [:= :session_id id]}))

(defn update-user-password
  [ds id password]
  (db/execute-one! ds
                   {:update [:users]
                    :set    {:password (bh/derive password)}
                    :where  [:= :id id]}))

(defn delete-user-by-id
  [ds id]
  (db/execute-one! ds
                   {:delete-from [:users]
                    :where       [:= :id id]}))
