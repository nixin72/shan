(ns shan.edit
  (:require
   [clojure.java.shell :as shell]
   [shan.config :as c]))

(defn cli-edit [& args]
  (shell/sh (System/getenv "EDITOR") c/conf-file))
