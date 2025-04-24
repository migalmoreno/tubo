(ns tubo.models.channel
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn add-channel
  [values ds]
  (jdbc/execute! ds
                 (-> {:insert-into [:channels]
                      :columns     [:url :name :avatar :verified]
                      :values      values}
                     sql/format)
                 {:return-keys true
                  :builder-fn  rs/as-unqualified-kebab-maps}))

(defn get-channel-by-url
  [url datasource]
  (jdbc/execute-one! datasource
                     (-> {:select [:*]
                          :from   [:channels]
                          :where  [:= :url url]}
                         sql/format)
                     {:builder-fn rs/as-unqualified-kebab-maps}))
