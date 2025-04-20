{:store :database
 :db    {:dbtype "postgresql"
         :dbname (System/getenv "PGDATABASE")
         :user   (System/getenv "PGUSER")}}
