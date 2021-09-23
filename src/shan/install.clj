(ns shan.install
  (:require
   [clojure.data :as data]
   [shan.util :as u]
   [shan.managers.managers :as pm]))

(defn cli-install [{:keys [verbose temp _arguments]}]
  (let [install-map (u/flat-map->map _arguments (first (first (u/get-new))))
        result (reduce-kv #(assoc %1 %2 (pm/install %2 %3 verbose)) {} install-map)
        failed (reduce-kv
                (fn [a k v]
                  (assoc a k (->> v (filter #(not= 0 (:exit %))) (mapv :package))))
                {}
                result)
        [success] (data/diff install-map failed)]
    ;; TODO Print failures
    (if temp
      (u/add-to-temp success)
      (u/add-to-conf success))))
