(ns tubo.db
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

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
