(ns shan.install
  (:require
   [shan.util :as u]
   [shan.managers.managers :as pm]))

(defn cli-install [{:keys [verbose temp _arguments]}]
  (let [install-map (u/flat-map->map _arguments)]
    (if temp
      (u/add-to-temp install-map)
      (u/add-to-conf install-map))
    (reduce-kv #(assoc %1 %2 (pm/install %2 %3 verbose)) {} install-map)))
