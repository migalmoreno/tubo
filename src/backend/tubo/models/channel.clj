(ns tubo.models.channel
  (:require
   [tubo.db :as db]))

(defn add-channels
  [values ds]
  (db/execute! ds
               {:insert-into [:channels]
                :columns     [:url :name :avatar :verified]
                :values      values}))

(defn get-channels-by-urls
  [ds urls]
  (db/execute! ds
               {:select [:*]
                :from   [:channels]
                :where  [:in :url urls]}))

(defn get-channel-by-url
  [url ds]
  (db/execute-one! ds
                   {:select [:*]
                    :from   [:channels]
                    :where  [:= :url url]}))

(defn delete-channels-by-ids
  [ds ids]
  (db/execute-one! ds
                   {:delete-from [:channels]
                    :where       [:in :id ids]}))
