(ns tubo.db
  (:require
   [honey.sql :as sql]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]
   [next.jdbc.result-set :as rs]
   [migratus.core :as migratus]
   [tubo.config :as config])
  (:import
   com.zaxxer.hikari.HikariDataSource))

(defn plan
  [ds sql]
  (jdbc/plan ds (sql/format sql) {:builder-fn rs/as-unqualified-kebab-maps}))

(defn execute!
  ([ds sql]
   (execute! ds sql nil))
  ([ds sql options]
   (jdbc/execute! ds
                  (sql/format sql)
                  (or options
                      {:return-keys true
                       :builder-fn  rs/as-unqualified-kebab-maps}))))

(defn execute-one!
  [ds sql]
  (jdbc/execute-one! ds
                     (sql/format sql)
                     {:return-keys true
                      :builder-fn  rs/as-unqualified-kebab-maps}))

(defmethod ig/init-key ::pg
  [_ _]
  (let [ds (connection/->pool HikariDataSource (config/get :db))]
    (migratus/migrate {:store :database :db {:datasource ds}})
    ds))

(defmethod ig/halt-key! ::pg
  [_ conn]
  (.close conn))
