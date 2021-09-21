(ns shan.edit
  (:require
   [clojure.java.shell :as shell]
   [shan.config :as c]))

(defn cli-edit []
  ;; TODO: Shell out to $EDITOR
  (->> (shell/sh "/bin/sh" "-c" "$EDITOR" c/conf-file) :out))
