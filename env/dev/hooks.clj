(ns hooks
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]))

(defn copy-assets
  {:shadow.build/stage :configure}
  [build-state {:keys [public-dir assets]}]
  (doseq [[src dest] assets]
    ((if (fs/directory? src) fs/copy-tree fs/copy)
     src
     (str public-dir "/" dest)
     {:replace-existing true
      :copy-attributes  true
      :nofollow-links   true}))
  build-state)

(defn webpack-watch
  {:shadow.build/stage :configure}
  [build-state]
  (proc/process {:extra-env {"NODE_ENV" "development"}}
                "./node_modules/.bin/webpack --watch")
  build-state)

(defn webpack-release
  {:shadow.build/stage :configure}
  [build-state]
  (proc/shell {:extra-env {"NODE_ENV" "production"}}
              "./node_modules/.bin/webpack")
  build-state)
