(ns shan.commands.add-archive
  (:require [shan.util :as u]
            [shan.commands.-options :as opts]))

(defn cli-add-ppa [{:keys [_arguments]}]
  (let [new-conf (u/get-new)
        ppas (or (:ppa new-conf) {})
        install-map (u/flat-map->map _arguments (:default-manager new-conf))]))

(def command
  {:command "add-archive"
   :short "aa"
   :category "Managing Packages"
   :arguments *
   :description "Adds package archives for your package managers to use"
   :runs cli-add-ppa
   :opts [opts/check? opts/temporary?]})
