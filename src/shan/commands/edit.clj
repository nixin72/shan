(ns shan.commands.edit
  (:require
   [shan.print :as p]
   [shan.config :refer [app-config]]
   [shan.util :as u]
   [shan.commands.-options :as opts]))

(defn- cli-edit
  "Launches a subprocess to edit your shan.edn file."
  [& _]
  (u/sh-verbose (System/getenv "EDITOR") (:conf-file @app-config))
  p/exit-code)

(def command
  {:command "edit"
   :short "ed"
   :arguments? 0
   :description "Shells out to a text editor for you to edit your config"
   :opts [opts/editor]
   :runs cli-edit})
