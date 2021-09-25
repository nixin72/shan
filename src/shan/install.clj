(ns shan.install
  (:require
   [shan.util :as u]
   [shan.managers.managers :as pm]))

(defn cli-install [{:keys [verbose temp _arguments]}]
  (let [new-conf (u/get-new)
        install-map (u/flat-map->map _arguments (:default new-conf))
        result (reduce-kv #(assoc %1 %2 (pm/install %2 %3 verbose)) {} install-map)
        {:keys [success failed]}
        (reduce-kv (fn [a k v]
                     (let [{:keys [s f]} (reduce
                                          #(if %2
                                             (update % :s conj %2)
                                             (update % :f conj %2))
                                          {:s [] :f []}
                                          v)]
                       (-> (assoc-in a [:success k] s)
                           (assoc :failed (seq f)))))
                   {:success {} :failed false}
                   result)]
    (when failed
      (println "\nPackages that failed to install were not added to your config."))
    (if temp
      (u/add-to-temp success)
      (u/add-to-conf success))))
