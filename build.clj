(ns build
  (:require
   [shadow.cljs.devtools.api :as shadow]
   [clojure.tools.build.api :as b]))

(def lib 'tubo)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn aot-compile [_]
  (println "Compiling AOT namespaces...")
  (b/compile-clj {:basis basis
                  :src-dir ["src"]
                  :class-dir class-dir
                  :ns-compile ['tubo.downloader-impl]})
  (println "Compiled AOT namespaces"))

(defn uberjar [_]
  (clean nil)
  (aot-compile nil)
  (b/copy-dir {:src-dirs ["src/clj" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dir ["src"]
                  :class-dir class-dir})
  (shadow/release :tubo)
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'tubo.core})
  (println "Uberjar: " uber-file))
