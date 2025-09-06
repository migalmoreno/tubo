(ns tubo.storage
  (:require
   ["localforage" :as localforage]
   [cognitect.transit :as transit]
   [malli.error :as me]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [re-frame.db :as db]
   [tubo.schemas :as s]))

(defn json->clj
  [json]
  (transit/read (transit/reader :json) json))

(def persist
  (rf/->interceptor
   :id    :persist
   :after (fn [context]
            (update-in context [:effects :fx] #(conj (or % []) [:persist])))))

(def schema-validator
  (rf/->interceptor
   :id    :schema-validator
   :after (fn [context]
            (update-in context
                       [:effects :fx]
                       #(conj (or % []) [:validate (:coeffects context)])))))

(rf/reg-fx
 :persist
 (fn []
   (let [persisted-db (select-keys @db/app-db s/persisted-local-db-keys)]
     (when-let [json (try (transit/write (transit/writer :json) persisted-db)
                          (catch :default e
                            #(rf/dispatch [:notifications/error e])))]
       (-> (localforage/setItem "tubo" json)
           (p/catch #(rf/dispatch [:notifications/error %])))))))

(rf/reg-fx
 :validate
 (fn [{:keys [db event]}]
   (when-not (s/local-db-valid? db)
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Local DB spec check failed: "
                            (me/humanize (s/local-db-explain db))))))))

(rf/reg-fx
 :fetch-store
 (fn [{:keys [on-success on-error on-finally]}]
   (-> (localforage/getItem "tubo")
       (p/then #(when on-success (rf/dispatch (conj on-success (json->clj %)))))
       (p/catch #(when on-error (rf/dispatch (conj on-error %))))
       (p/finally #(when on-finally (rf/dispatch on-finally))))))

(rf/reg-fx
 :clear-store
 (fn [db]
   (js/console.error (js/Error. (str "Local DB spec check failed: "
                                     (me/humanize (s/local-db-explain db)))))
   (localforage/clear)))

(comment
  (rf/reg-global-interceptor schema-validator)
  (rf/clear-global-interceptor :schema-validator))
