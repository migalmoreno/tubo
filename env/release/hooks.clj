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

(defn postcss
  {:shadow.build/stage :configure}
  [build-state src dst]
  (let [cmd ["./node_modules/.bin/postcss" src "-o" dst "--verbose"]]
    (case (:shadow.build/mode build-state)
      :dev     (apply proc/process (conj cmd "-w"))
      :release (apply proc/shell {:extra-env {"NODE_ENV" "production"}} cmd)))
  build-state)
