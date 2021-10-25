(ns shan.archive
  (:require
   [shan.util :as u]))

(defn cli-add-ppa [{:keys [_arguments]}]
  (let [new-conf (u/get-new)
        ppas (or (:ppa new-conf) {})
        install-map (u/flat-map->map _arguments (:default-manager new-conf))]))

(defn cli-del-ppa [{:keys [_arguments]}]
  (let [new-conf (u/get-new)
        ppas (or (:ppa new-conf) {})
        remove-map (u/flat-map->map _arguments (:default-manager new-conf))]))
