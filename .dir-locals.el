;;; Directory Local Variables            -*- no-byte-compile: t -*-
;;; For more information see (info "(emacs) Directory Variables")

((nil . ((fill-column . 80)
         (compile-command . "nix run .")
         (cider-preferred-build-tool . clojure-cli)
         (cider-clojure-cli-aliases . ":dev:cljs")
         (cider-default-cljs-repl . custom)
         (cider-custom-cljs-repl-init-form . "(do (require '[shadow.cljs.devtools.api :as shadow])\n(require '[shadow.cljs.devtools.server :as server])\n(server/start!)\n(shadow/watch :frontend)\n(shadow/nrepl-select :frontend))")
         (cider-merge-sessions . :project)
         (eval . (progn
                   (unless (boundp 'cider-jack-in-nrepl-middlewares) (require 'cider))
                   (make-variable-buffer-local 'cider-jack-in-nrepl-middlewares)
                   (add-to-list 'cider-jack-in-nrepl-middlewares
                                "cider.nrepl/cider-middleware")
                   (add-to-list 'cider-jack-in-cljs-nrepl-middlewares
                                "shadow.cljs.devtools.server.nrepl/middleware"))))))
