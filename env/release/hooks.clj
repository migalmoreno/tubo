(ns hooks
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]))

(defn copy-assets
  {:shadow.build/stage :configure}
  [build-state {:keys [public-dir assets]}]
  (doseq [[src dst] assets]
    ((if (fs/directory? src) fs/copy-tree fs/copy)
     src
     (str public-dir "/" dst)
     {:replace-existing true
      :copy-attributes  true
      :nofollow-links   true}))
  build-state)

(defn postcss-watch
  {:shadow.build/stage :configure}
  [build-state]
  (proc/process
   {:extra-env {"NODE_ENV" "development"}}
   "./node_modules/.bin/postcss" "resources/src/index.css"
   "-o"                          "resources/public/index.css"
   "--verbose"                   "-w")
  build-state)

(defn postcss-release
  {:shadow.build/stage :configure}
  [build-state]
  (proc/shell {:extra-env {"NODE_ENV" "production"}}
              "./node_modules/.bin/postcss"
              "resources/src/index.css"    "-o"
              "resources/public/index.css" "--verbose")
  build-state)

(defn esbuild-watch
  {:shadow.build/stage :configure}
  [build-state]
  (fs/create-dirs "target/js/")
  (when-not (fs/exists? "target/js/index.js")
    (fs/create-file "target/js/index.js"))
  (proc/process "node" "scripts/build-libs.mjs" "--watch")
  build-state)

(defn esbuild-release
  {:shadow.build/stage :flush}
  [build-state]
  (proc/shell "node" "scripts/build-libs.mjs")
  build-state)
