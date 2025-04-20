(ns tubo.handlers.auth
  (:require
   [buddy.hashers :as bh]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn create-signup-handler
  [{:keys [datasource body-params]}]
  (if (jdbc/execute-one! datasource
                         (-> {:select [:*]
                              :from   [:users]
                              :where  [:= :username (:username body-params)]}
                             sql/format))
    {:status 500
     :body   "User with that username already exists"}
    (let [account (jdbc/execute-one!
                   datasource
                   (-> {:insert-into :users
                        :columns     [:username :password]
                        :values      [[(:username body-params)
                                       (bh/derive (:password body-params))]]}
                       sql/format)
                   {:return-keys true
                    :builder-fn  rs/as-unqualified-kebab-maps})]
      {:status  200
       :body    account
       :session (select-keys account [:username :created-at])})))

(defn create-logout-handler
  [_]
  {:status  200
   :session nil})

(defn create-login-handler
  [{:keys [datasource body-params]}]
  (if-let [account (jdbc/execute-one!
                    datasource
                    (-> {:select [:*]
                         :from   [:users]
                         :where  [:= :username (:username body-params)]}
                        sql/format)
                    {:builder-fn rs/as-unqualified-kebab-maps})]
    (if (:valid (bh/verify (:password body-params) (:password account)))
      {:status  200
       :body    account
       :session (select-keys account [:username :created-at])}
      {:status 500
       :body   "Wrong password for user"})
    {:status 500
     :body   "Couldn't find user with that username"}))
