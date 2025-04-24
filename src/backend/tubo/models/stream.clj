(ns tubo.models.stream
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn add-stream
  [values ds]
  (jdbc/execute! ds
                 (-> {:insert-into [:streams]
                      :columns     [:duration :uploader_url :name :thumbnail
                                    :url]
                      :values      values}
                     sql/format)
                 {:return-keys true
                  :builder-fn  rs/as-unqualified-kebab-maps}))

(defn get-stream-by-url
  [url ds]
  (jdbc/execute-one! ds
                     (-> {:select [:*]
                          :from   [:streams]
                          :where  [:= :url url]}
                         sql/format)
                     {:builder-fn rs/as-unqualified-kebab-maps}))
