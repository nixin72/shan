(ns shan.commands.remove-archive
  (:require
   [shan.util :as u]
   [shan.commands.-options :as opts]))

(defn cli-del-ppa [{:keys [_arguments]}]
  (let [new-conf (u/get-new)
        ppas (or (:ppa new-conf) {})
        remove-map (u/flat-map->map _arguments (:default-manager new-conf))]))

(def command
  {:command "rem-archive"
   :short "ra"
   :category "Managing Packages"
   :arguments *
   :description "Removes package archives from your package managers"
   :runs cli-del-ppa
   :opts [opts/check? opts/temporary?]})
