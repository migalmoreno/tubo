;;; Directory Local Variables            -*- no-byte-compile: t -*-
;;; For more information see (info "(emacs) Directory Variables")

((nil . ((fill-column . 80)
         (cider-preferred-build-tool . clojure-cli)
         (cider-clojure-cli-aliases . "-M:frontend")
         (cider-default-cljs-repl . custom)
         (cider-custom-cljs-repl-init-form . "(do (require '[shadow.cljs.devtools.api :as shadow])\n(require '[shadow.cljs.devtools.server :as server])\n(server/start!)\n(shadow/watch :tubo)\n(shadow/nrepl-select :tubo))")
         (cider-merge-sessions . :project)
         (eval . (if
                     (not
                      (boundp 'cider-jack-in-nrepl-middlewares))
                     (require 'cider)
                   (make-variable-buffer-local 'cider-jack-in-nrepl-middlewares)
                   (add-to-list 'cider-jack-in-nrepl-middlewares "cider.nrepl/cider-middleware")
                   (add-to-list 'cider-jack-in-nrepl-middlewares "shadow.cljs.devtools.server.nrepl/middleware"))))))
